# JSON Response Format

A proposal for the format of the JSON response for a requested Mesh node.

## TODO

* How should we deal with sort order and sorted by fields?
  * Use object as indicated in example below with the following changes: 
    * For non-node types, the "orderBy" property does not apply.
    * Fields can be used for sorting by using the JSON path to those fields, e.g. `fields.name`. When a field cannot be utilized (allowed sorting properties to be defined) , an exception is thrown.
* If user attempts to update a property which should be not updateable (e.g. a "creator" property) an exception is thrown.
* Add a flag / property to indicate whether the node is a container.

## Example 1 - Product schema


### Example Schema 1

Consider the following "product" schema definition:

```json
{
  "name": "product",
  "fields": [
    {
      "name": "name",
      "label": "Product Name",
      "type": "string",
      "required": true,
    },
    {
      "name": "description",
      "label": "Description",
      "type": "html",
      "required": true,
    },
    {
      "name": "image",
      "label": "Product Image",
      "type": "node",
      "allow": ["image"]
    },
    {
      "name": "sku",
      "label": "SKU",
      "type": "string",
      "required": true
    },
    {
      "name": "price",
      "label": "Price",
      "type": "number",
      "step": 0.01
     },
     {
      "name": "relatedProducts",
      "label": "Related Products",
      "type": "list",
      "listType": "node",
      "allow": ["product"]
      }
  ]
}
```

This is what a response object could look like, specifying English and German for any localized fields:

### Response 1

#### `GET api/v1/products/aeroplane?lang=en,de&expand=image`

```json
{
   "uuid": "e0c64ad00a9343cc864ad00a9373cc23",
   "language": "en",
   "availableLanguages": ["en", "de"],
   "tags": [],
   "creator":{
      "uuid": "UUIDOFUSER1",
      "lastname": "Doe",
      "firstname": "Joe",
      "username": "joe1",
      "emailAddress": "j.doe@spam.gentics.com",
      "groups": [],
      "perms": []
   },
   "fields":{
      "name": "Aeroplane",
      "description": "A good aeroplane.",
      "image": {
         "uuid": "a8u328u23u8r09j23o9r09",
         "language": "en",
         "availableLanguages": ["en", "de"],
         "tags": [],
         "creator": {},
         "path": "/products/aeroplane/plane.jpg",
         "fileName": "plane.jpg",
         "binaryProperties": {
            "width": 400,
            "height": 800,
            "fileSize": 1241324,
            "mimeType": "image/jpeg",
            "dpi": 72
         },
         "fields": {
           "title": "Front view of the plane"
         },
         "perms": [],
         "schema": {}
      },
      "sku": "3898-990",
      "price": 13223.00,
      "relatedCategory": { 
         "uuid": "235hr9283yr98239822430f"
      },
      "relatedProducts": {
         "order": "desc",
         "orderBy": "name",
         "items": [
             { "uuid": "235hr9283yr98239823410f" },
             { "uuid": "38wq3jo39r20jr029j3h838" },
             { "uuid": "21982ej081320eu1092u309" }
         ]
   },
   "perms":[
      "read",
      "create",
      "update",
      "delete"
   ],
   "schema":{
      "schemaName": "product",
      "schemaUuid": "4776dcfee87745f3b6dcfee87705f3ad"
   }
}
```

### Sample Angular code for example 1

Here is how an AngularJS app might consume the above JSON response when rendering a product page:

#### Controller

```javascript
// the "product" argument is the above JSON converted to a JS object
function productPageController(product) {
  var vm = this;

  vm.product = product.fields;
  vm.lang = product.language;
  vm.relatedProducts = product.fields.relatedProducts.forEach(uuidToName)
    
  // this is only required because we did not expand the "relatedProducts" field.
  // In practice it would be simpler to expand the field.
  function uuidToName(uuid) {
    var name;
    // resolve the uuid to the name of the node
    return name;
  }
}
```

#### View
```xml
<div class="product-page">

  <h1>{{ vm.product.name }}</h1>

  <div class="product-description">
    {{ vm.product.description }}
  </div>

  <div class="product-image-container">
    <img ng-src="{{ vm.product.image.path }}">
    <span class="caption">
      {{ vm.product.image.fields.title }}
    </span>
  </div>

  <div class="product-details">
    <span class="price">{{ vm.product.price }}</span>
    <span class="sku">{{ vm.product.sku }}</span>
  </div>

  <h2>Related Products</h2>
  <ul>
    <li ng-repeat="productName in vm.relatedProducts">
      {{ productName }}
    </li>
  </ul>

</div>
```
  
## Example 2 - Blog post with microschemas

### Example Schema 2

```javascript
{
  "name": "blogPost",
  "fields": [
    {
      "name":  "title",
      "label": "Title",
      "type":  "string",
    },
    {
      "name":  "content",
      "label": "Content",
      "type":  "list",
      "listType": "microschema",
      "allow": ["htmlBlock", "captionedImage"]
    }
  ]
}
```

#### Microschemas used in blog post schema

```javascript
// microschema for an HTML block
{
  "name": "htmlBlock",
  "fields": [
    {
      "name": "content",
      "label": "Content",
      "type": "html"
    }
  ]
}
```

```javascript
// microschema for image with caption
{
  "name": "captionedImage"
  "fields": [
    {
      "name": "image",
      "label": "Image",
      "type": "node",
      "allow": ["image"]
    },
    {
      "name": "caption",
      "label": "Caption",
      "type": "string"
    }
  ]
}
```

The user then creates a new blog post and places in the "content" field:

1. an htmlBlock,
2. a captionedImage
3. another htmlBlock


### Response 2

#### `GET api/v1/blogPosts/myPost?expand=content`
("creator" & "perms" truncated for clarity)

```json
{
   "uuid":"e0c64ad00a9343cc864ad00a9373cc23",
   "language": "en",
   "availableLanguages": ["en", "de"],
   "tags":[],
   "creator":{},
   "fields": {
     "title": "My First Blog Post"
     "content": [
       {
         "microschema": {
           "name": "htmlBlock",
           "uuid": "sef98s34h8r29384823894h"
         },
         "fields": {
           "content": "First paragraph..."
         }
       },
       {
         "microschema": {
           "name": "captionedImage",
           "uuid": "42349jrj2902ruu9w000"
         },
         "fields": {
           "image": {
              "uuid": "a8u328u23u8r09j23o9r09",
              "language": "en",
              "availableLanguages": ["en", "de"],
              "tags": [],
              "creator": {},
              "path": "/products/aeroplane/plane.jpg",
              "fileName": "plane.jpg",
              "binaryProperties": {
                 "width": 400,
                 "height": 800,
                 "fileSize": 1241324,
                 "mimeType": "image/jpeg",
                 "dpi": 72
              },
              "fields": {
                "title": "Front view of the plane"
              },
              "perms": [],
              "schema": {}
           },
           "caption": "Check out this image!"
         }
       },
       {
         "microschema": {
           "name": "htmlBlock",
           "uuid": "sef98s34h8r29384823894h"
         },
         "fields": {
           "content": "Second paragraph...",
         },
       }
     ]
   },
   "perms":[],
   "schema":{
      "schemaName":"blogPost",
      "schemaUuid":"4776dcfee87745f3b6dcfee87705f3ad"
   }
}
```

### Sample Angular code for example 2

Here is how an AngularJS app might consume the above JSON response when rendering a blog post including microschemas:

#### Controller

```javascript
// the "post" argument is the above JSON converted to a JS object
function blogPostController(post) {
  var vm = this;

  vm.post = post.fields;
  vm.lang = post.language;
}
```

#### View
```xml
<div class="blog-page">

  <h1>{{ vm.post.title }}</h1>

  <div class="post-body">
    <div ng-repeat="contentBlock in vm.post.content">
      <div ng-switch="contentBlock.microschema.name">
      
        <div ng-switch-when="htmlBlock">
            <!-- render the HTML block -->
        	<p>{{ contentBlock.fields.content }}<p>
        </div>
        
        <div ng-switch-when="captionedImage">
        	<!-- render the image and caption -->
            <img ng-src="contentBlock.fields.image.path">
            <span class="caption">{{ contentBlock.fields.image.fields.caption }}</span>
        </div>
        
      </div>
    </div>
  </div>

</div>
```
