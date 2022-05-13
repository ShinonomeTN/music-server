[#-- @ftlvariable name="modalJson" type="String" --]

<html lang="en-US">
<head>
    <title>Music Server OAuth - Confirm</title>
    [#include "common_headers.ftl"]
</head>
<body>
<div id="app" class="container-sm">

</div>
<script src="https://unpkg.com/vue@3.2.31"></script>
<script type="text/x-template" id="PermissionListView">
    <div class="flex-grow-1 msc-flex-column" style="overflow: hidden;">
        <div style="font-weight: bold; font-size: large; text-align: center" class="m-3">{{userAgent}}</div>
        <div class="m-3" style="text-align: left">
            <div>This app wants to have accesses to following resources:</div>
            <div class="mt-1">
                <div class="p-1" v-for="(item, index) in scopeList" :key="index" style="display: flex; align-items: center">
                    <div style="padding: 2pt 10pt">
                        <i class="bi bi-braces-asterisk" style="font-size: 32pt"></i>
                    </div>
                    <div style="flex-grow: 1">
                        <div style="font-weight: bold">{{item.title}}</div>
                        <div style="font-size: smaller">{{item.description}}</div>
                    </div>
                </div>
            </div>
        </div>
        <form action="/api/auth?action=allow" method="post" style="width: 100%">
            <input type="hidden" name="__ts" id="__ts" value="[=modal.sessionSigned]"/>
            <button class="btn btn-primary" style="width: 100%">Allow</button>
        </form>
        <button class="btn btn-dark" @click="$emit('disallow')">Disallow</button>
    </div>
</script>
<script type="text/x-template" id="DisallowView">
    <div class="flex-grow-1 msc-flex-column" style="overflow: hidden;">
        <div style="text-align: center; flex-grow: 1" class="m-3">
            <div>Request was rejected.</div>
            <div>Nothing to do. You can close this window now.</div>
        </div>
        <button class="btn btn-danger" @click="closeWindow">Close Window</button>
    </div>
</script>
<script>
  window.$MusicServer = [=modalJson]
</script>
<script src="auth/config/scope_descriptions.js"></script>
<script>
  window.$app = Vue.createApp({
    components: {
      PermissionListView: {
        template: `#PermissionListView`,
        props: ["userAgent", "scopeList"]
      },
      DisallowView : {
        template: `#DisallowView`,
        methods: {
          closeWindow() {
            window.close()
          }
        }
      }
    },
    template: `
      <div style="display: flex; justify-content: center">
      <div class="msc-card msc-main msc-flex-column" style="flex-grow: 1">
        <div style="display: flex; align-items: center">
          <h1>Authorization</h1>
          <div style="flex-grow: 1"></div>
          <img v-if="user.avatar" :src="user.avatar" class="img-thumbnail" width="25" height="25" style="padding: 0 5pt"/>
          <div v-else style="overflow: hidden; border-radius: 1pt; padding: 0 5pt">
            <i class="bi bi-person-fill" style="font-size: 24pt"></i>
          </div>
          <div>{{ user.nickname || user.username }}</div>
        </div>
        <component @disallow='state = "disallow-view"' :is="state" :userAgent="session.userAgent" :scopeList="scopeList"/>
      </div>
      </div>
    `,
    data() {
      return {
        state: "permission-list-view"
      }
    },
    mounted() {

    },
    computed: {
      user() {
        return $MusicServer.user
      },
      session() {
        return $MusicServer.session
      },
      scopeDescriptionJson() {
        return $MusicServer.scopeDescriptions
      },
      scopeList() {
        const scope = $MusicServer.session.scope
        if (!scope) return []
        return scope.map(i => {
          const item = this.scopeDescriptionJson[i]
          if (!item) return {
            title: i,
            description: null
          }; else return item
        })
      }
    }
  })

  window.onload = (function () {
    window.$app.mount("#app")
  })
</script>
</body>
</html>
