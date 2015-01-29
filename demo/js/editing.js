function enableAloha() {
  var editable = $('.main_content');
  editable.css('border-style','solid');
  editable.css('border-width', '5px');
  aloha(editable[0]);
  $('.aloha-caret').show();
}

function disableAloha() {
  var ele = $('.main_content');
  console.dir(ele[0]);
//  aloha.mahalo(ele[0]);
  ele.before(ele.clone());
  ele.remove();
  $('.main_content').css('border-style','');
  $('.main_content').css('border-width','');
  $('.aloha-caret').hide();
}

function saveContent() {
  var ele = $('.main_content');
  var html = ele.html();
  var data = { 'content':  html};
  
  $.ajax({
      type        :   'POST',
      url         :   "/proxy-php/api/v1/page/save/" + window.pageId,
      data        :   JSON.stringify(data),
      contentType :   'application/json',
      success     :   function(response) {
          console.log(response);
      }
  });

}

$(document).ready(function() {

  var ctrlDown = false;

  $(document).keydown(function(e) {
    if (e.ctrlKey) ctrlDown = true;
  }).keyup(function(e) {
    if (e.ctrlKey) ctrlDown = false;
  });

  $(document).keydown(function(e) { 
    //console.log(e.which);
    //console.log(ctrlDown);
    if (ctrlDown && (e.which === 83)) {
       saveContent();
       disableAloha();
       e.preventDefault();
       return false;
    }
    if (ctrlDown && (e.which === 69)) {
        //console.log("you pressed ");
        enableAloha();
        e.preventDefault();
        return false;
    }
  });

});
