# Setup
* Build this plugin with `mvn clean package -DskipTests`. A fat jar will be located in the target folder
* Create a folder called `plugins` in your mesh directory if it does not exist yet
* Put the fat jar inside your `plugins` folder
* Create the folders `plugins/forms`, `plugins/forms/templates`
* Create a file `plugins/forms/config.yml`. See the configuration section for more details
* Create handlebars templates in your `plugins/forms/templates` folder
* To enable mail sending:
  * Configure `mail` settings in `config.yml`
  * Add `fromAddress` in `config.yml`
  * Create templates for usermails and adminmails
  * Configure paths to templates as `adminMailTemplate` and `userMailTemplate` in `config.yml` (without the .hbs extension)
  * Configure `adminMailSubject` and `userMailSubject` in `config.yml`


# API

A specification of the API can be found in the file `api.yaml`. You can paste the contents of that file to https://editor.swagger.io/ to get a nice overview.

# Configuration
## Fields
| Key                           | Type                 | Description                                                                                                                            | Default value             |
|-------------------------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------|---------------------------|
| `templatesPath`               | string               | base path to the templates                                                                                                             | `plugins/forms/templates` |
| `elements`                    | map of strings       | Definition of custom elements. Each entry maps the `type` of the custom element to the implementation class                            |                           |
| `mail`                        | object               | Mail settings                                                                                                                          |                           |
| `fromAddress`                 | string               | Address to use as `from` when sending mails                                                                                            |                           |
| `adminMailTemplate`           | string               | Path to the default template for admin mails, relative to `templatesPath`. Template name must be set **without** extension `.hbs`.     |                           |
| `adminMailSubject`            | string               | Subject for admin mails. Placeholder [[FORMNAME]] will be replaced with the name of the form                                           |                           |
| `userMailTemplate`            | string               | Path to the default template for user mails, relative to `templatesPath`. Template name must be set **without** extension `.hbs`.      |                           |
| `userMailSubject`             | string               | Subject for user mails. Placeholder [[FORMNAME]] will be replaced with the name of the form                                            |                           |
| `defaultExportFormat`         | string               | Default export format. Currently, only `CSV` is supported                                                                              | CSV                       |
| `reCaptcha`                   | object               | Recaptcha settings                                                                                                                     |                           |
| `friendlyCaptcha`             | object               | FriendlyRecaptcha settings                                                                                                             |                           |
| `eyepin`                      | object               | Eyepin settings                                                                                                                        |                           |
| `proxyOptions`                | object               | Proxy settings, if http proxy is required for accessing the recaptcha/friendlycaptcha verification URL                                 |                           |
| `fileUpload`                  | object               | Definition of allowed filetypes/filenames per selected `allowed_filetypes` attribute of the `file` element                             |                           |
| `fileUpload.allowedFileTypes` | map of string arrays | Keys are `types` of allowed files, maps are lists of regular expressions for matching the filetypes against                            |                           |
| `fileUpload.allowedFileNames` | map of string arrays | Keys are `types` of allowed files, maps are lists of regular expressions for matching the filenames against                            |                           |
| `fileUpload.defaultAllowed`   | string array         | List of `types` (from the configuration above), which are allowed by default (unless overwritten by `allowed_filetypes` of an element) |                           |
| `custom`                      | map of objects       | Custom configurations you can use in your extensions and handlebars template                                                           |                           |

## Example File
```yml
templatesPath: "plugins/forms/templates"
elements:
  custom: com.acme.CustomElement
mail:
  hostname: localhost
  port: 25
  authMethods:
  username:
  password:
  ssl: false
  trustAll: false
  keepAlive: true
  maxPoolSize: 10
  ownHostname:
  attachFileUploads: false
fromAddress: "webmaster@gentics.com"
adminMailTemplate: "adminemail"
adminMailSubject: "Formular [[FORMNAME]] ausgefüllt"
userMailTemplate: "useremail"
userMailSubject: "Herzlichen Dank"
defaultExportFormat: CSV
reCaptcha:
  siteKey: "your-site-key"
  secretKey: "your-secret-key"
friendlyCaptcha:
  siteKey: "your-site-key"
  secretKey: "your-secret-key"
eyepin:
  apiUrl: "https://apiv3.eyepin.com/interface3.php"
  username: "your-username"
  password: "your-password"
proxyOptions:
  host: proxy.acme.com
  port: 4711
  user: proxyuser
  password: proxypassword
  active: false
fileUpload:
  allowedFileTypes:
    all:
      - ".*"
    image:
      - "image/.*"
    audio:
      - "audio/.*"
    video:
      - "video/.*"
  allowedFileNames:
    archive:
      - ".*\\.zip"
      - ".*\\.rar"
      - ".*\\.7z"
      - ".*\\.bz"
      - ".*\\.bz2"
      - ".*\\.gz"
    documents:
      - ".*\\.doc"
      - ".*\\.docx"
      - ".*\\.xls"
      - ".*\\.xlsx"
      - ".*\\.pdf"
      - ".*\\.txt"
      - ".*\\.ods"
      - ".*\\.odt"
  defaultAllowed:
    - "all"
custom:
  test: "test"
  testobject:
    testfield: "another test field"
```

# Form properties

The following properties are used by the plugin itself (although some _may_ be empty or `null`):

| Key | Type | Description                                                                           | Default value |
|-----|------|---------------------------------------------------------------------------------------|---------------|
| `name` | string | Name of the form                                                                      | REQUIRED |
| `email` | string | Comma separated list of admin email addresses                                         | `null` |
| `mailsubject` | string | Mail subject for overriding `adminMailSubject` from the configuration                 | `null` |
| `mailtemp` | string | Path to a mail template for overriding `adminMailTemplate` from the configuration     | `null` |
| `mailsource` | string | Mail template for overriding `mailtemp` property or `adminMailTemplate` configuration | `null` |
| `successurl` | string | URL for redirection after user successfully filled the form                           | `null` |
| `elements` | array of objects | Array of embedded elements                                                            | `[]` |

Other custom elements provided in the form JSON will still be stored and made available during rendering.

# Default Elements

## Common properties

| Key | Type | Description | Default value |
|-----|------|-------------|---------------|
| `name` | string | Field name | REQUIRED |
| `type` | string | Field type | REQUIRED |
| `label` | string | Field label | `null` |
| `active` | boolean | Flag to activate/deactivate fields. Deactivated fields will not be rendered in the form any more (but previously POSTed data is still available) | `true` |
| `multivalue` | boolean | Flag for multivalue fields | `false` |
| `mandatory` | boolean | Flag for mandatory field | `false` |
| `validation` | string | Validation method for POSTed data | `null` |
| `elements` | array of objects | Array of embedded elements (for container elements) | `[]` |

Every component may have arbitrary additional properties, which will only be used for rendering.

## `input`

Element for (single line) input fields.

No extra properties.

## `checkbox`

Element for checkbox fields.

No extra properties.

## `textarea`

Element for (multi line) textarea fields.

No extra properties.

## `combielem`

Base element for elements which represent a list of predefined options to select. The `select_type` is intended to be either `checkbox` or `radiobutton`. For rendering purposes the value for `select_type` can be any string, but for the element to be considered multivalue, the `select_type` must be `checkbox`.

| Key           | Type | Description                                                                                                                   | Default value |
|---------------|------|-------------------------------------------------------------------------------------------------------------------------------|---------------|
| `options` | selectable_options | Key-value pairs used as options for the combi element                                                                         |REQUIRED|
| `select_type` | string | The type of representation. Should be `checkbox` or `radiobutton` (with checkbox meaning the element accespts multiple values | `null`         |

## `buttons`

Element which renders the form buttons.

No extra properties.

## `formpage`

Container element for multi-page forms.

No extra properties.

## `usermail`

Element for user`s email address.
When filled, the plugin will send an email to the entered address after the form was successfully filled.

| Key | Type | Description                                                                               | Default value |
|-----|------|-------------------------------------------------------------------------------------------|---------------|
| `mailsubject` | string | Mail subject for overriding `userMailSubject` from the configuration                      | |
| `mailtemplate` | string | Mail template for overriding `userMailTemplate` from the configuration                    | |
| `mailsource` | string | Mail template for overriding `mailtemplate` property and `userMailTemplate` configuration | |

If property `mailsource` is set to type `REPOSITORY_BROWSER`, it is possible for CMS editors to configure a special CMS page as template for the email body.
Check [e-mail-template-pages](https://www.gentics.com/Content.Node/guides/feature_forms.html#e-mail-template-pages) on how to create these special pages in the CMS.

Example configuration of the usermail element inside the Gentics CMS [form-editor.json](https://gentics.com/Content.Node/guides/feature_forms.html#configuration):

```json
        {
            "type": "usermail",
            "type_label_i18n_ui": {
                "en": "User EMail Field",
                "de": "Benutzer E-Mail Feld"
            },
            "description_i18n_ui": {
                "en": "Field for user eMail. When filled, the user will get an eMail after filling out the form.",
                "de": "Feld für Benutzer eMail. Wenn ausgefüllt, bekommt der Benutzer ein eMail nach dem Absenden des Formulares."
            },
            "label_property_ui": "label",
            "is_container": false,
            "properties": [
                {
                    "name": "label",
                    "type": "STRING",
                    "label_i18n_ui": {
                        "en": "Label",
                        "de": "Label"
                    },
                    "description_i18n_ui": {
                        "en": "Label for this input field.",
                        "de": "Label für dieses Eingabefeld."
                    }
                },
                {
                    "name": "mandatory",
                    "type": "BOOLEAN",
                    "label_i18n_ui": {
                        "en": "Mandatory",
                        "de": "Verpflichtend"
                    },
                    "description_i18n_ui": {
                        "en": "This input field is mandatory.",
                        "de": "Dieses Eingabefeld ist verpflichtend."
                    }
                },
                {
                    "name": "placeholder",
                    "type": "STRING",
                    "label_i18n_ui": {
                        "en": "Placeholder",
                        "de": "Platzhalter"
                    },
                    "description_i18n_ui": {
                        "en": "Placeholder for this input field.",
                        "de": "Platzhalter für dieses Eingabefeld."
                    }
                },
                {
                    "name": "maxCharCount",
                    "type": "NUMBER",
                    "label_i18n_ui": {
                        "en": "Input Length",
                        "de": "Eingabelänge"
                    },
                    "description_i18n_ui": {
                        "en": "Maximum number of allowed characters.",
                        "de": "Anzahl der Zeichen, die eingegeben werden dürfen."
                    }
                },
                {
                    "name": "mailsubject",
                    "type": "STRING",
                    "label_i18n_ui": {
                        "en": "Email Subject",
                        "de": "E-Mail Betreff"
                    },
                    "description_i18n_ui": {
                        "en": "Subject of the Email, sent to the user.",
                        "de": "Betreff des E-Mails, das an den Benutzer geschickt wird."
                    }
                },
                {
                    "name": "mailtemplate",
                    "type": "SELECT",
                    "label_i18n_ui": {
                        "en": "Email Template",
                        "de": "E-Mail Vorlage"
                    },
                    "description_i18n_ui": {
                        "en": "Template, which is used to display the Email body.",
                        "de": "Vorlage, die zur Darstellung des E-Mail Inhalts verwendet wird."
                    },
                    "options": [
                        {
                            "key": "useremail",
                            "value_i18n_ui": {
                                "en": "Default",
                                "de": "Standard"
                            }
                        }
                    ]
                },
                {
                    "name": "mailsource",
                    "type": "REPOSITORY_BROWSER",
                    "label_i18n_ui": {
                        "en": "Email Template from Page",
                        "de": "E-Mail Vorlage als Seite"
                    },
                    "description_i18n_ui": {
                        "en": "Template from Page, which is used to display the Email body.",
                        "de": "Vorlage als Seite, die zur Darstellung des E-Mail Inhalts verwendet wird."
                    },
                    "options": {
                        "allowedSelection": "page",
                        "includeMlId": [21],
                        "selectMultiple": false,
                        "title": "modal.repository_browser_title_forms_email_template_single"
                    }
                }
            ]
        }
```

## `adminmail`

Element to specify to which admin mail addresses submission notifications should be sent. If such an element is present in the form, the forms `email` property will be ignored, and instead be taken from the selected values.

*IMPORTANT:* Make sure to not render the actual email addresses in the elements template. Only the keys of the available options should be rendered.

When the `select_type` starts with the string `multiselect` the element is treated as a multivalue element. The values themselves can be lists of mail addresses separated by commas.

| Key           | Type | Description                                                                                                                                        | Default value |
|---------------|------|----------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `options` | selectable_options | Key-value pairs used as options for the admin mail element                                                                                         |REQUIRED|
| `select_type` | string | The type of representation. The type is mostly used for rendering, if the element should accept multiple values, it has to start with `multiselect` | `null`         |

## `mandatorygroup`

Container element for mandatory group of elements. At least one of the embedded elements has to be filled by the user.

No extra properties.

## `selectgroup`

Element for select group. The difference to the `combielem` is the `additional_element` field for entering values not covered by the options, and how the element determines if it is multivalue. A `selectgroup` will accept multiple values if the `select_type` starts with `multivalue`.

| Key | Type | Description | Default value |
|-----|------|-------------|---------------|
| `select_type` | string or select | Determines whether a selectgroup is a multivalue field or not. Selectgroups with a select_type that starts with the string "multiselect" are considered to be multivalue(e.g. "multiselect", "multiselect_checkboxes", "multiselect_switch",...). |REQUIRED|
| `options` | selectable_options | Key-value pairs used as options for the select group |REQUIRED|
| `additional_element` | boolean | Flag for letting the user add additional elements | `false` |

When `additional_element` is `true`, and the POSTed data contains `[fieldname]=[fieldname]additional_field`, the custom value POSTed as `[fieldname]_additional` will be taken.

Handlebars examples:
* [selectgroup_multiselect.hbs](src/main/resources/templates/elements/selectgroup_multiselect.hbs)
* [selectgroup_singleselect.hbs](src/main/resources/templates/elements/selectgroup_singleselect.hbs)
* [selectgroup_radiobuttons.hbs](src/main/resources/templates/elements/selectgroup_radiobuttons.hbs)

## `recaptcha`

Element for recaptcha.

No extra properties.

## `file`

Element for file uploads.

| Key | Type | Description | Default value |
|-----|------|-------------|---------------|
| `max_filesize` | integer | Maximum allowed file size in MB (0 means unlimited) | 0 |
| `allowed_filetypes` | array of strings | Allowed filetypes (see configuration). `null` for using default configuration. | `null` |

## `conditionalcontainer`

Container for elements to show on condition.

When the current value of `elementToCheck` is not equal to `valueToCheck`, the forms plugin will

1. Skip validation for all elements inside the container, and
2. Ignore posted values for elements inside the container.

Displaying or hiding the elements must be handled client side, a simple example for this can be found in the example template: _src/main/resources/templates/elements/conditionalcontainer.hbs_.

### Configuration options

| Key | Type | Description | Default value |
|-----|------|-------------|---------------|
| `elementToCheck` | string | Name of the element to check for specific value | REQUIRED |
| `valueToCheck` | string | Required value of the element to check | |

### CMS configuration

This is an example configuration for a `conditionalcontainer` element to add to the forms `elements` in the [Gentics CMS form-editor.json](https://gentics.com/Content.Node/guides/feature_forms.html#configuration).

```json
{
  "type": "conditionalcontainer",
  "type_label_i18n_ui": {
    "en": "Conditional container",
    "de": "Wenn-Dann-Container"
  },
  "description_i18n_ui": {
    "en": "Conditional container which contents is only active when the checked element has the expected value.",
    "de": "Wenn-Dann-Container dessen Inhalt nur aktiv ist, wenn das geprüfte Element den erwarteten Wert hat."
  },
  "label_property_ui": "label",
  "is_container": true,
  "properties": [
    {
      "name": "label",
      "type": "STRING",
      "label_i18n_ui": {
        "en": "Label",
        "de": "Label"
      },
      "description_i18n_ui": {
        "en": "Label for this container.",
        "de": "Label für diesen Container."
      }
    },
    {
      "name": "element_to_check",
      "type": "STRING",
      "label_i18n_ui": {
        "en": "Element name",
        "de": "Elementname"
      },
      "description_i18n_ui": {
        "en": "Element to check.",
        "de": "Zu prüfendes Element."
      }
    },
    {
      "name": "value_to_check",
      "type": "STRING",
      "label_i18n_ui": {
        "en": "Value",
        "de": "Wert"
      },
      "description_i18n_ui": {
        "en": "Expected value.",
        "de": "Erwarteter Wert."
      }
    }
  ]
}
```

# Server-side Validation

Server-side validation will check whether mandatory fields have been filled by the user. Furthermore, entered values will be checked against the field's `validation`:

| validation | Description |
|------------|-------------|
| `number` | Accepts integer numbers |
| `telephone` | Accepts telephone numbers matching regex pattern ^[\s\d\+\/]*$ |
| `socialsecurity` | Accepts austrian social security numbers matching regex pattern ^\s*(\d{4})(?\<date\>\d{6})\s*$ |
| `date` | Accepts dates entered in format yyyy-MM-dd |
| `time` | Accepts times entered in format HH:mm |
| `email` | Accepts email addresses |
| `datepicker` | Accepts dates entered in format dd.MM.yyyy |

# Friendly Captcha

Mandatory settings for the friendlyCaptcha to work are `siteKey` and `secretKey`

## Configuration Options
These are the options available for the FriendlyCaptcha Implementation 

| Key                   | Type     | Description                                                                                                                             | Default value |
|-----------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------|---|
| `siteKey`             | string   | Key from the FriendlyCaptcha                                                                                                            | empty | 
| `secretKey`           | string   | Secret from FriendlyCaptcha                                                                                                             | empty |  
| `solutionKey`         | string   | Key for the field containing the hash of the verification                                                                               | frc-captcha-solution  | 
| `verificationUrl`     | string   | URL against which the captcha is verified                                                                                                | https://api.friendlycaptcha.com/api/v1/siteverify | 
| `acceptOnErrorStatus` | boolean  | Activates recommended behaviour as [mentioned here](https://docs.friendlycaptcha.com/#/verification_api?id=verification-best-practices) | false |

## Endpoint
The endpoint for the EU which is [mentioned here](https://docs.friendlycaptcha.com/#/eu_endpoint) must be activated and
the Worldwide one can be deactivated, so check that before using Friendlycaptcha.

## Configuration inside of the CMS
FriendlyCaptcha provides configurable behaviour for the Widget on a website. 

Use this example configuration for the Gentics CMS form-editor.json [here](https://gentics.com/Content.Node/guides/feature_forms.html#configuration)
to allow editors to set the start and lang attribute when adding a FriendlyCaptcha Element to the form

```json
{
  "type": "friendlycaptcha",
  "type_label_i18n_ui": {
    "en": "Friendly Captcha",
    "de": "Friendly Captcha"
  },
  "description_i18n_ui": {
    "en": "Friendly Captcha spam protection.",
    "de": "Friendly Captcha Spamschutz."
  },
  "is_container": false,
  "properties": [
    {
      "name": "start",
      "type": "SELECT",
      "label_i18n_ui": {
        "en": "Captcha Start",
        "de": "Start des Captchas"
      },
      "description_i18n_ui": {
        "en": "Defines when the widget should be started.",
        "de": "Bestimmt wann das Captcha starten soll."
      },
      "options": [
        {
          "key": "auto",
          "value_i18n_ui": {
            "en": "After page load",
            "de": "Nach Seitenaufbau"
          }
        },
        {
          "key": "focus",
          "value_i18n_ui": {
            "en": "Focus on the form",
            "de": "Fokus auf das Formular"
          }
        },
        {
          "key": "none",
          "value_i18n_ui": {
            "en": "Started by the user",
            "de": "Vom User gestartet"
          }
        }
      ]
    },
    {
      "name": "lang",
      "type": "SELECT",
      "label_i18n_ui": {
        "en": "Language",
        "de": "Sprache"
      },
      "description_i18n_ui": {
        "en": "Language used inside of the Captcha.",
        "de": "Sprache innerhalb des Captchas"
      },
      "options": [
        {
          "key": "de",
          "value_i18n_ui": {
            "en": "German",
            "de": "Deutsch"
          }
        },
        {
          "key": "en",
          "value_i18n_ui": {
            "en": "English",
            "de": "Englisch"
          }
        },
        {
          "key": "fr",
          "value_i18n_ui": {
            "en": "French",
            "de": "Französisch"
          }
        },
        {
          "key": "it",
          "value_i18n_ui": {
            "en": "Italian",
            "de": "Italienisch"
          }
        },
        {
          "key": "nl",
          "value_i18n_ui": {
            "en": "Dutch",
            "de": "Niederländisch"
          }
        },
        {
          "key": "pt",
          "value_i18n_ui": {
            "en": "Portuguese",
            "de": "Niederländisch"
          }
        },
        {
          "key": "es",
          "value_i18n_ui": {
            "en": "Spanish",
            "de": "Niederländisch"
          }
        },
        {
          "key": "ca",
          "value_i18n_ui": {
            "en": "Catalan",
            "de": "Katalanisch"
          }
        },
        {
          "key": "da",
          "value_i18n_ui": {
            "en": "Danish",
            "de": "Dänisch"
          }
        },
        {
          "key": "ja",
          "value_i18n_ui": {
            "en": "Japanese",
            "de": "Japanisch"
          }
        }
      ]
    }
  ]
}
```

# Eyepin

Mandatory settings for the Eyepin newsletter registration to work are `apiUrl`, `username` and `password`.

## Configuration Options
These are the options available for the FriendlyCaptcha Implementation 

| Key                   | Type     | Description                                                                                                                             | Default value |
|-----------------------|----------|-----------------------------------------------------------------------------------------------------------------------------------------|---|
| `apiUrl`             | string   | URL of the Eyepin API 
| `username`           | string   | Username for the Eyepin account  
| `password`         | string   | Password for the Eyepin account

## Configuration inside of the CMS
A form that contains an eyepin element must contain a usermail element as well. The eyepin element inside the form generator must have a customer id set.  

Use this example configuration for the Gentics CMS form-editor.json [here](https://gentics.com/Content.Node/guides/feature_forms.html#configuration):

```json
{
    "type": "eyepin",
    "type_label_i18n_ui": {
        "en": "Eyepin newsletter registration",
        "de": "Eyepin Newsletteranmeldung"
    },
    "description_i18n_ui": {
        "en": "Eyepin newsletter registration. The form must also contain a user email field.",
        "de": "Eyepin Newsletteranmeldung. Das Formular muss zusätzlich ein Benutzer E-Mail Feld enthalten."
    },
    "label_property_ui": "label",
    "is_container": false,
    "properties": [
        {
            "name": "label",
            "type": "STRING",
            "label_i18n_ui": {
                "en": "Label",
                "de": "Label"
            },
            "description_i18n_ui": {
                "en": "Label for this selection.",
                "de": "Label für diese Auswahl."
            }
        },
        {
            "name": "mandatory",
            "type": "BOOLEAN",
            "label_i18n_ui": {
                "en": "Mandatory",
                "de": "Verpflichtend"
            },
            "description_i18n_ui": {
                "en": "This checkbox is mandatory.",
                "de": "Dieses Checkbox ist verpflichtend."
            }
        },
        {
            "name": "checked",
            "type": "BOOLEAN",
            "label_i18n_ui": {
                "en": "Checked",
                "de": "Ausgewählt"
            },
            "description_i18n_ui": {
                "en": "This checkbox is checked.",
                "de": "Dieses Checkbox ist ausgewählt."
            }
        },
        {
            "name": "customerId",
            "type": "STRING",
            "label_i18n_ui": {
                "en": "Eyepin customer id (required)",
                "de": "Eyepin Mandanten ID (Pflichtfeld)"
            },
            "description_i18n_ui": {
                "en": "Customer ID in eyepin",
                "de": "ID des Mandanten in Eyepin."
            }
        },
        {
            "name": "value",
            "type": "STRING",
            "label_i18n_ui": {
                "en": "Value",
                "de": "Wert"
            },
            "description_i18n_ui": {
                "en": "Value that is selected by this checkbox.",
                "de": "Wert der durch diese Checkbox gewählt wird."
            },
            "default_value_i18n": {
                "en": "X",
                "de": "X",
                "pl": "X",
                "cs": "X",
                "hu": "X",
                "sl": "X",
                "ar": "X",
                "bg": "X",
                "bs": "X",
                "da": "X",
                "es": "X",
                "et": "X",
                "fa": "X",
                "fi": "X",
                "fr": "X",
                "hr": "X",
                "it": "X",
                "ja": "X",
                "ko": "X",
                "mk": "X",
                "nl": "X",
                "pt": "X",
                "ro": "X",
                "ru": "X",
                "sk": "X",
                "sq": "X",
                "sr": "X",
                "tr": "X",
                "uk": "X",
                "ar_eg": "X",
                "en_ca": "X",
                "fr_ca": "X"
            }
        }
    ]
}

```

# Form rendering

When using server-side rendering, the form will be rendered with the template `<templatesPath>/form.hbs`.

The form plugin will put relevant information in form of a JSON object in the render context under the name `form`, which looks like this:

```json
{
  "id": "<FORM_UUID>",
  "language": "en",
  "multipage": false,
  "options": { /* Plugin configuration */ },
  "pathPrefix": "/prefix",
  "elements": [ /* Form elements */ ]
}
```

Where
* `id` is the UUID of the form node
* `language` is the language of the form
* `multipage` is `true` when there is at least one `formpage` element in the form, and `false` otherwise
* `options` is a JSON representation of the plugin configuration
* `pathPrefix` is the path prefix of the projects default branch
* `elements` contains the active form elements

Handlebars example for rendering a form:
* [form.hbs](src/main/resources/templates/form.hbs)

# Extending

For extending the forms-plugin, a new maven project must be created, that uses the forms-plugin as dependency and builds a custom mesh-plugin, containing the forms-plugin together with the custom extension classes.

## Example pom.xml
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.gentics</groupId>
  <artifactId>custom-forms-plugin</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <properties>
    <mesh.version>0.31.4</mesh.version>
    <vertx.version>3.5.4</vertx.version>
    <forms.plugin.version>1.6.16</forms.plugin.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>com.gentics.mesh</groupId>
      <artifactId>mesh-plugin-api</artifactId>
      <version>${mesh.version}</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>com.gentics.mesh</groupId>
      <artifactId>mesh-rest-client</artifactId>
      <version>${mesh.version}</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>io.vertx</groupId>
      <artifactId>vertx-web-templ-handlebars</artifactId>
      <version>${vertx.version}</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>io.vertx</groupId>
      <artifactId>vertx-mail-client</artifactId>
      <version>${vertx.version}</version>
      <scope>provided</scope>
    </dependency>

    <dependency>
         <groupId>com.gentics.mesh.plugin.commercial</groupId>
         <artifactId>mesh-forms-plugin</artifactId>
         <version>${forms.plugin.version}</version>
     </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-shade-plugin</artifactId>
        <executions>
          <!-- Run shade goal on package phase -->
          <execution>
            <phase>package</phase>
            <goals>
              <goal>shade</goal>
            </goals>
            <configuration>
              <transformers>
                <transformer
                  implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                  <manifestEntries>
                    <Main-Verticle>service:com.gentics.mesh.plugin.plugin-service</Main-Verticle>
                  </manifestEntries>
                </transformer>
              </transformers>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
  <repositories>
        <repository>
            <id>maven.gentics.com</id>
            <name>Gentics Maven Repository</name>
            <url>https://maven.gentics.com/maven2</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
        <repository>
            <id>gtx-commercial</id>
            <name>Gentics Maven Commercial Repository</name>
            <url>https://repo.apa-it.at/artifactory/gtx-maven-releases-commerical/</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>
</project>
```

## Example src/main/resources/mesh-plugin.json
```json
{
  "name": "Custom Forms Plugin",
  "apiName": "forms",
  "description": "Custom Plugin for management of forms",
  "license": "Commercial",
  "inception": "2019-05-14",
  "author": "Gentics",
  "version": "1.0"
}
```

## Custom Elements
* Custom elements can be implemented by extending class `com.gentics.mesh.plugin.forms.model.FormElement`
* Mapping for custom element types to classes must be added to `elements` in the configuration file `config.yml`

# Handlebars helpers

The following helpers are available for rendering mail bodies or HTML markup:

* `formsElementTemplate ELEMENT_TYPE`: returns the path to the template partial for the given type (when rendering forms)
* `pollResultTemplate ELEMENT_TYPE`: returns the path to the template partial for the given type (when rendering poll results)
* `formsInputType INPUT_ELEMENT`: returns the suggested input type for the given element
* `formatDate DATE TARGET_FORMAT [SOURCE_FORMAT]`: returns the given date formatted with TARGET_FORMAT. When no SOURCE_FORMAT is specified, the input is assumed to be in the form YYY-MM-DD.
* `formatTime TIME TARGET_FORMAT [SOURCE_FORMAT]`: same as `formatDate` for a time. When no SOURCE_FORMAT is specified the input is assumed to be in the form HH:mm. Note that both `formatDate` and `formatTime` can be used to format with any pattern parsable by the [DateTimeFormatter](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html) class. The distinct helpers are just provided with different default source formats for convenience reasons.

* startsWith checks if string variable starts with match string
```handlebars
{{#startsWith element.name 'hidden_element'}}
    <!-- true action here -->
  {{else}}
    <!-- false action here -->
{{/startsWith}}
```
