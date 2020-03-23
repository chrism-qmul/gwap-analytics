var GWAPAnalytics = {
  buildQueryString: function(params) {
    var queryString = "";
    for(var i = 0; i < params.length; i++) {
      if (i != 0) {
        queryString += "&";
      }
      queryString += encodeURIComponent(params[i][0]) + '=' + encodeURIComponent(params[i][1]);
    }
    return queryString;
  },

  getUrlParameter: function(name) {
    name = name.replace(/[\[]/, '\\[').replace(/[\]]/, '\\]');
    var regex = new RegExp('[\\?&]' + name + '=([^&#]*)');
    var results = regex.exec(location.search);
    return results === null ? '' : decodeURIComponent(results[1].replace(/\+/g, ' '));
  },

  generateClientId: function() {
    return "GA" + (Math.random() + "." + (new Date()).getTime()).substring(2);
  },

  deleteCookie: function(name) {
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:00 UTC;'
  },

  setCookie: function(name, value) {
    document.cookie = name + '=' + value + '; expires=Fri, 31 Dec 9999 23:59:59 GMT;path=/;';
  },

  getCookieValue: function(name) {
    var cookies = document.cookie.split(";");
    for(var i = 0; i < cookies.length; i++) {
      var string_start = 0;
      for(; string_start < cookies[i].length; string_start++) {
        if (cookies[i].charAt(string_start) !== ' ') {
          break;
        }
      }
      if (cookies[i].indexOf(name + '=') === string_start) {
        return cookies[i].substring(string_start + (name + '=').length);
      }
    }
  },

  listenEvent: function(el, event_type) {
    if (el.addEventListener) {
        el.addEventListener(event_type, modifyText, false); 
    } else if (el.attachEvent)  {
        el.attachEvent('on' + event_type, modifyText);
    }
  },

  persistQueryVariable: function(name) {
    var value = GWAPAnalytics.getCookieValue(name);
    if (!value) {
      value = GWAPAnalytics.getUrlParameter(name);
      if (value) {
        value = GWAPAnalytics.setCookie(name, value);
      }
    }
  },

  setup: function(player) {
    GWAPAnalytics.persistQueryVariable("utm_campaign");
    GWAPAnalytics.persistQueryVariable("utm_experiment");
    var cid = GWAPAnalytics.getCookieValue("_gwapa_cid");
    if (!cid) {
      cid = GWAPAnalytics.generateClientId();
      GWAPAnalytics.setCookie("_gwapa_cid", cid);
    }
    if (player) {
      GWAPAnalytics.setCookie("player", player);
    }
  },

  send: function(data) {
    var url = "https://gwap-analytics.com/analytics";
    //console.log("sending", data);
    if (navigator.sendBeacon) {
      var formdata = new FormData();
      for(var i = 0; i < data.length; i++) {
        formdata.append(data[i][0], data[i][1]);
      }
      navigator.sendBeacon(url, formdata);
    } else {
      var querystring = "";
      for(var i = 0; i < data.length; i++) {
        querystring += encodeURIComponent(data[i][0]) + "=" + encodeURIComponent(data[i][1]) + "&";
      }
      querystring += "t=" + Math.random();
      if (window.XMLHttpRequest) {
        //console.log("xmlhttp", url);
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.open("POST", url, true);
        xmlhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        //xmlhttp.withCredentials = true;
        xmlhttp.send(querystring);
      } else {
        var img = new Image();
        img.src = url + "?" + querystring;
      }
    }
  },

  getDefaultParameters: function() {
    var defaults = [["host", window.location.host]];
    var campaign = GWAPAnalytics.getCookieValue("utm_campaign");
    var experiment = GWAPAnalytics.getCookieValue("utm_experiment");
    var player = GWAPAnalytics.getCookieValue("player") || GWAPAnalytics.getCookieValue("_gwapa_cid");
    if (campaign) {
      defaults.push(["campaign", encodeURIComponent(campaign)]);
    }
    if (experiment) {
      defaults.push(["experiment", encodeURIComponent(experiment)]);
    }
    defaults.push(["player", encodeURIComponent(player)]);
    return defaults;
  },

  sendevent: function(type) {
    var data = GWAPAnalytics.getDefaultParameters();
    data.push(["type", type]);
    GWAPAnalytics.send(data);
  },

  startgame: function() {
    GWAPAnalytics.sendevent("start");
  },

  endgame: function() {
    GWAPAnalytics.sendevent("end");
  },

  judgement: function() {
    GWAPAnalytics.sendevent("judgement");
  },
};
GWAPAnalytics.setup();
