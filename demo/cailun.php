<?php

namespace Gentics\CaiLunDemo;

class CaiLunPHPDemo {

    private $user;
    private $password;

    public function __construct($user, $password) {
        $this->user = $user;
        $this->password = $password;
    }

    /**
    * Return the baseurl which includes the basic authentication information.
    */
    private function getBaseURL() {
        return "http://" . $this->user . ":" . $this->password . "@localhost:8080/api/v1";
    }

   /**
    * Handle the request and load the matching page using cailun 
    */
    public function handleRequest() {
        $actualPath = "$_SERVER[REQUEST_URI]";

        $url =  $this->getBaseURL() . "/tag/get". $actualPath;
        $json =  file_get_contents($url); 
        $data= json_decode($json);
        if ($this->isDebug()) {
            var_dump($data);
            echo $url;
        }
        echo $this->renderTemplate("post.tpl", $data);
    }

    private function isDebug() {
        return isset($_GET['debug']);
    }

   /**
    * Render the template by replacing all placeholders with the loaded data.
    * Return the rendered page.
    */
    private function renderTemplate($template, $data) {
        $template = file_get_contents(__DIR__ . "/templates/" . $template);

        if (is_null($data)) {
            http_response_code(404);
            $content = "Page not found";
            $title   = "Not found";
        } else {
            // load extra data
            $navigation = $this->renderNavigation();
            $date       = "December 01, 2014";
            $content    = $data->object->content;
            $id         = $data->object->id;
            $name       = $data->object->name;
            $title      = $data->object->title;
            $teaser     = $data->object->teaser;
            $author     = $data->object->author;
        }

        $rendered = str_replace(
                        array(
                            "%cailun.page.id%",
                            "%cailun.navigation%",
                            "%cailun.page.name%",
                            "%cailun.page.content%",
                            "%cailun.page.date%",
                            "%cailun.page.title%",
                            "%cailun.page.teaser%",
                            "%cailun.page.author%",
                        ), array(
                            $id === null         ? "" : $id,
                            $navigation === null ? "" : $navigation,
                            $name === null       ? "" : $name,
                            $content  === null   ? "" : $content,
                            $date === null       ? "" : $date,
                            $title === null      ? "" : $title,
                            $teaser === null     ? "" : $teaser,
                            $author === null     ? "" : $author
                        ), $template);
        return $rendered;
    }

    private function printNav($element, $level = 0) {
        $nElements = count($element->children);
        $isPage = $element->type == "PAGE";
        if ($level==0) {
            echo "<ul>\n";
        }
        $link = '<a href="' . $element->path . '" target="' . $element->target . '">'.$element->name . '</a>';
        echo "<li>" . $link . "</li>\n";
        if ($nElements != 0) {
            echo "<ul>\n";
            $nextLevel = $level + 1;
            foreach ($element->children as $key => $value) {
                $this->printNav($value, $nextLevel);
            }
            echo "</ul>\n";
            echo "</li>\n";
        }
        if ($level == 0) {
            echo "</ul>\n";
        }
    }

    private function renderNavigation() {
        $url = $this->getBaseURL() . "/nav/get";
        $json =  file_get_contents($url); 
        $navData = json_decode($json);
        if ($this->isDebug()) {
            var_dump($navData);
            echo $url;
        }
        ob_start();
        $this->printNav($navData->root, 0);
        $nav = ob_get_clean();
        return $nav;
    }

}
