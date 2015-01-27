<?php
    namespace Gentics\CaiLunDemo;
    
    require_once('cailun.php');
    
    $cailun = new CaiLunPHPDemo("joe1", "test123");
    $cailun->handleRequest();