[#-- @ftlvariable name="modelJson" type="String" --]
<html lang="en-US">
<head>
    <title>Music Server OAuth - Error</title>
    [#include "common/common_headers.ftl"]
</head>
<body>
<div id="app" class="container-sm">
    <div style="display: flex; justify-content: center">
        <div class="msc-card msc-main msc-flex-column" style="flex-grow: 1">
            <h1>Error</h1>
            <div class="flex-grow-1 msc-flex-column" style="overflow: hidden">
                <component :message="errorMessageForUser" :is="errorMessageForTarget"></component>
            </div>
            <button class="btn mt-3" :class="[item.color]" v-for="(item, index) in recoverList" :key="index" @click="item.action">{{item.text}}</button>
        </div>
    </div>
</div>
[#include "common/common_scripts.ftl"]
<script src="/config/i18n.js"></script>
<script type="text/x-template" id="UserErrorPage">
    <div class="mt-3" style="text-align: center">
        <h2>{{message.title}}</h2>
        <h3>{{message.description}}</h3>
    </div>
</script>
<script type="text/x-template" id="DeveloperErrorPage">
    <div style="text-align: center" class="mt-3 msc-flex-column center">
        <h3>Some error Happened.</h3>
        <div id="qrcode" class="m-5">

        </div>
        <div class="mb-3">If you need helps, show this QRCode to the app developer.</div>
    </div>
</script>
<script type="text/x-template" id="MaintainerErrorPage">
    <div style="text-align: center" class="mt-3 msc-flex-column center">
        <h3>Some error Happened.</h3>
        <div id="qrcode" class="m-5">

        </div>
        <div class="mb-3">If you need helps, show this QRCode to maintainers.</div>
    </div>
</script>
<script type="text/x-template" id="UnknownErrorPage">
    <div style="text-align: center" class="mt-3 msc-flex-column center">
        <h3>Some error Happened.</h3>
        <div>Unfortunately, no help message available.</div>
        <div>Please just close this page.</div>
    </div>
</script>
<script>
  window.$MusicServer = [=modelJson]

  const buttons = {
    "retry" : {
      text : "Go Back",
      color : "btn-primary",
      action: function() {
        history.back()
      }
    },
    "reject" : {
      text: "Close Window",
      color: "btn-danger",
      action: function () {
        window.close()
      }
    }
  }

  window.$app = Vue.createApp({
    components: {
      "user" : {
        template : `#UserErrorPage`,
        props : {
          message : {
            type : Object,
            default: () => ({})
          }
        }
      },
      "maintainer" : {
        template: `#MaintainerErrorPage`
      },
      "developer" : {
        template: `#DeveloperErrorPage`
      },
      "unknown" : {
        template: `#UnknownErrorPage`
      }
    },
    filters: {
      i18n(value) {
        return $i18n.translateError(value)
      }
    },
    data() {
      return {
        errorDetails : $MusicServer.error.details,
        errorMessage : $MusicServer.error
      }
    },
    mounted() {
        switch(this.errorMessageForTarget) {
          case "maintainer" :
          case "developer" :
            this.createQRCode()
            break;
          default:
            break;
        }
    },
    methods: {
      createQRCode() {
        const qrCode = new QRCodeStyling({
          width: 200,
          height: 200,
          type: "svg",
          data: atob(this.errorMessage.message),
          dotsOptions: {
            color: getComputedStyle(document.documentElement).getPropertyValue("--bs-light"),
            type: "rounded"
          },
          backgroundOptions: {
            color: getComputedStyle(document.documentElement).getPropertyValue("--bs-dark"),
          },
          imageOptions: {
            crossOrigin: "anonymous",
            margin: 20
          }
        });

        qrCode.append(document.getElementById("qrcode"));
      }
    },
    computed: {
      recoverList() {
        if(!this.errorDetails) return []
        if(!this.errorDetails.recover) return []
        return this.errorDetails.recover.map(i => buttons[i]).filter((i) => !!i)
      },
      errorMessageForTarget() {
        return this.errorDetails.to || "unknown"
      },
      errorMessageForUser() {
        return $i18n.translateError(this.errorDetails)
      }
    }
  })
  window.onload = (function(){
    window.$app.mount("#app")
  })
</script>
</body>
</html>