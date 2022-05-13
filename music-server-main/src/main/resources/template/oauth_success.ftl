[#-- @ftlvariable name="modalJson" type="String" --]
<html lang="en-US">
<head>
    <title>Music Server OAuth - Finished</title>
    [#include "common_headers.ftl"]
</head>
<body>
<div id="app" class="container-sm">
    <div style="display: flex; justify-content: center">
        <div class="msc-card msc-main msc-flex-column" style="flex-grow: 1">
            <h1>Authorization Success</h1>
            <div class="flex-grow-1 msc-flex-column" style="overflow: hidden; text-align: center; justify-content: center">
                <div>You have successfully granted the permissions to the application.</div>
                <div>Now just wait for the application to proceed.</div>

                <div class="mt-5">If the application does not respond, click the button below to retry.</div>
            </div>
            <button class="btn btn-outline-dark" @click="refresh">Retry</button>
        </div>
    </div>
</div>
[#include "common_scripts.ftl"]
<script>
  window.$MusicServer = [=modalJson]

  window.$app = Vue.createApp({
    methods: {
      refresh() {
        location.reload()
      }
    }
  })
  window.onload = (function(){
    window.$app.mount("#app")
  })
</script>
</body>
</html>
