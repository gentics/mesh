package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.forms.config.EyepinOptions;
import com.gentics.mesh.plugin.forms.config.ProxyOptions;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.Route;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Locale;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Eyepin newsletter registration element
 */
@Reportable
public class EyepinElement extends FormElement {

    @JsonProperty
    private String value;

    @JsonProperty
    private String customerId;

    @JsonProperty
    private String listIds;

    @JsonProperty
    private String topicIds;

    public EyepinElement() {
    }

    protected EyepinElement(EyepinElement other, boolean deep) {
        super(other, deep);

        this.value = other.value;
        this.customerId = other.customerId;
        this.listIds = other.listIds;
        this.topicIds = other.topicIds;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getListIds() {
        if (listIds != null) {
            return listIds;
        } else {
            return "";
        }
    }

    public void setListIds(String listIds) {
        this.listIds = listIds;
    }

    public String getTopicIds() {
        if (topicIds != null) {
            return topicIds;
        } else {
            return "";
        }
    }

    public void setTopicIds(String topicIds) {
        this.topicIds = topicIds;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    protected FormElement clone(boolean deep) {
        return new EyepinElement(this, deep);
    }

    @Override
    public ValidationError validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode) {
        String value = getSingleFormData(postedData);

        ValidationError error = new ValidationError();
        if (StringUtils.isNotBlank(value)) {

            // eyepin element was checked, thus user mail must exist and have a value too
            boolean containsUserMail = postedData.entries().stream().anyMatch(v -> v.getKey().startsWith("usermail"));

            if (!containsUserMail) {
                error.setName(this.getName());
                error.setTitle(this.getLabel());
                error.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.eyepin.missing-usermail"));

                log.debug("Form has no usermail element thus eyepin element cannot be used.");

                return error;
            } else {
                postedData.forEach(e -> {
                    if (e.getKey().startsWith("usermail")) {
                        if (StringUtils.isBlank(e.getValue())) {
                            // user mail is not valid
                            error.setName(this.getName());
                            error.setTitle(this.getLabel());
                            error.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.eyepin.empty-usermail"));

                            log.debug("Usermail element has no value thus eyepin element cannot be used.");
                        }
                    }
                });
            }
        }

        if (StringUtils.isBlank(error.getName())) {
            return null;
        } else {
            return error;
        }
    }

    /**
     * Performs submit action
     */
    @JsonIgnore
    public void performSubmit(Form form, MultiMap postedData) {
        log.debug("Form submitted: Contains eyepin element.");

        String value = getSingleFormData(postedData);

        if (StringUtils.isNotBlank(value)) {

            // insert each user mail to eyepin addresses
            form.flattened().filter(e -> e instanceof UsermailElement).map(e -> (UsermailElement) e)
                    .forEach(element -> {
                        String userMail = element.getSingleFormData(postedData);

                        if (StringUtils.isBlank(userMail)) {
                            // TODO: return Warning
                            log.debug("User mail is empty. Skip inserting address to eyepin.");
                            return;
                        }
                        // add user mail to Eyepin addresses
                        insertAddressToEyepin(userMail);
                    });
        }

    }

    /**
     * Send a request to eyepin to insert the user email to the address list of the
     * customer id(=Mandant) that is configured in the form element.
     * <p>
     * https://docs.eyepin.com/api/address#address-insert
     *
     * @param userMail user mail that should be added to addresses in eyepin
     */
    public void insertAddressToEyepin(String userMail) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isBlank(userMail)) {
            log.debug("User mail is empty.");
        } else {
            log.debug("Trying to insert address to Eyepin: {}", userMail);

            EyepinOptions eyepinOptions = getConfiguration().getEyepin();
            String password = eyepinOptions.getPassword();
            String username = eyepinOptions.getUsername();
            String apiUrl = eyepinOptions.getApiUrl();

            String customerId = this.getCustomerId();
            String listIds = this.getListIds();
            String topicIds = this.getTopicIds();

            log.debug("Eyepin customer id: " + customerId + " listIds: " + listIds + " topicIds: " + topicIds);
            if (!StringUtils.isBlank(customerId)) {

                // TODO: Reuse created OkHttp clients, maybe add a respective provider in the
                // plugin commons.
                OkHttpClient client = new OkHttpClient();

                ProxyOptions proxyOptions = getConfiguration().getProxyOptions();

                // set proxy options
                if (proxyOptions != null && proxyOptions.isActive()) {
                    log.debug("Using proxy:\n{}", proxyOptions);
                    int proxyPort = Integer.parseInt(proxyOptions.getPort());
                    String proxyHost = proxyOptions.getHost();

                    String proxyPassword = proxyOptions.getPassword();
                    String proxyUsername = proxyOptions.getUser();

                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                    client = client.newBuilder().proxy(proxy).build();

                    if (!StringUtils.isBlank(proxyUsername) && !StringUtils.isBlank(proxyPassword)) {
                        Authenticator proxyAuthenticator = new Authenticator() {
                            @Override
                            public Request authenticate(Route route, Response response) throws IOException {
                                String credential = Credentials.basic(proxyUsername, proxyPassword);
                                return response.request().newBuilder().header("Proxy-Authorization", credential)
                                        .build();
                            }
                        };
                        client = client.newBuilder().proxyAuthenticator(proxyAuthenticator).build();
                    }
                }

                // prepare request
                MediaType mediaType = MediaType.parse("application/xml");
                String credential = Credentials.basic(username, password);
                sb.append("");
                if (topicIds != "" && topicIds != null) {
                    sb.append("\n\t<topicids merge=\"1\">\"" + topicIds + "\"</topicids>");

                }
                if (listIds != "" && listIds != null) {
                    sb.append("\n\t<lists merge=\"1\">");
                    String[] listIdsArray = listIds.split(",");
                    for (int i = 0; i < listIdsArray.length; i++) {
                        sb.append("\n\t<lists id=\""+ listIdsArray[i] + "\"/>");
                    }
                    sb.append("\"</lists>");
                }
                String additionalInfo = sb.toString();
                RequestBody body = RequestBody.create(mediaType,
                        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<addressinsert key=\"Email\" save-empty-fields=\"0\" overrule-blacklist=\"0\" customerid=\""
                                + customerId + "\">\n\t<email>" + userMail
                                + "</email>"
                                + additionalInfo
                                + "\n\t<status>1</status>\n  <handleoptin>1</handleoptin>\n</addressinsert>");
                Request request = new Request.Builder().url(apiUrl).post(body).addHeader("Content-Type", "text/xml")
                        .addHeader("Authorization", credential).build();

                if (log.isDebugEnabled()) {
                    log.debug("Body");
                    log.debug(body.toString());
                }
                try {
                    // send request
                    // TODO: rewrite to be a single (rxjava)
                    Response response = client.newCall(request).execute();
                    int responseCode = response.code();

                    if (responseCode == 200) {
                        handleResponse(response);
                    } else {
                        log.error("OkHttp POST to eyepin failed with code " + responseCode + ": " + response.message());
                    }
                } catch (IOException e) {
                    log.error("OkHttp POST to eyepin failed.");
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                log.error("Eyepin element has no customer id set. Address not inserted.");
            }
        }
    }

    /**
     * Parse the eyepin response and log its response code and description.
     *
     * @param respones XML response from eyepin
     */
    private void handleResponse(Response response) {

        // check response content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            ResponseBody responseBody = response.body();

            String xmlString = responseBody.string();
            log.debug("OkHttp POST to eyepin - response body: " + xmlString);

            db = factory.newDocumentBuilder();

            InputSource inStream = new InputSource();
            inStream.setCharacterStream(new StringReader(xmlString));
            Document doc = db.parse(inStream);

            NodeList nl = doc.getElementsByTagName("response");

            String responseBodyCode = "";
            String responseBodyDescription = "";
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {

                    NodeList childNodes = nl.item(i).getChildNodes();
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        if (childNodes.item(j).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                            String nodeName = childNodes.item(j).getNodeName();
                            String nodeTextContent = childNodes.item(j).getTextContent();
                            switch (nodeName) {
                                case "code":
                                    responseBodyCode = nodeTextContent;
                                    break;
                                case "description":
                                    responseBodyDescription = nodeTextContent;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
            if (responseBodyCode.equals("2000")) {
                log.debug("Eyepin response - code: " + responseBodyCode);
                log.debug("Eyepin response - description: " + responseBodyDescription);
            } else {
                log.error("Eyepin response - code: " + responseBodyCode);
                log.error("Eyepin response - description: " + responseBodyDescription);
            }
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
