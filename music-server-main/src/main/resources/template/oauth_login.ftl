[#-- @ftlvariable name="modelJson" type="String" --]

<html lang="en-US">
<head>
    <title>Music Server OAuth - Login</title>
    [#include "common/common_headers.ftl"]
</head>
<body>
<div id="app" class="container-sm">
    <div style="display: flex; justify-content: center">
        <div class="msc-card msc-main msc-flex-column" style="flex-grow: 1">
            <h1>Sign In</h1>
            <div class="flex-grow-1 msc-flex-column" style="overflow: hidden">
                <transition name="ani-fade-up" mode="out-in">
                    <component :is="state" :session="session"></component>
                </transition>
            </div>
        </div>
    </div>
</div>
[#include "common/common_scripts.ftl"]
<script type="text/x-template" id="LoginPrepareView">
    <div style="padding: 10pt 0; flex-grow: 1" class="msc-flex-row-center msc-flex-column center">
        <div class="msc-flex-column center">
            <div style="margin: 5pt" class="spinner-border text-light" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <div>
                <span>Preparing...</span>
            </div>
        </div>
    </div>
</script>
<script type="text/x-template" id="LoginPageFromView">
    <div style="flex-grow: 1; display: flex; flex-flow: column">
        <div>
            Sign into music server to continue.
        </div>
        <form style="flex-grow: 1; display: flex; flex-flow: column;" class="pt-3" method="post" :action="targetAction">
            <div style="flex-grow: 1">
                <div class="mb-3">
                    <label for="usernameInput" class="form-label">Username</label>
                    <input v-model="form.username.value" @focusout='checkForm(form.username)' name="username" id="username" placeholder="Username"
                           autocomplete="false" autocapitalize="off"
                           class="form-control" id="usernameInput" aria-describedby="emailHelp">
                    <div v-if="form.username.error" class="form-text text-danger">
                        {{form.username.error}}
                    </div>
                </div>
                <div class="mb-3">
                    <label for="exampleInputPassword1" class="form-label">Password</label>
                    <input v-model="form.password.value" @focusout='checkForm(form.password)' name="password" id="password" placeholder="Password"
                           type="password" class="form-control" id="exampleInputPassword1">
                    <div v-if="form.password.error" class="form-text text-danger">
                        {{form.password.error}}
                    </div>
                </div>
            </div>
            <div class="d-grid gap-3 pt-3">
                <button type="submit" :disabled="buttonDisabled" class="btn btn-primary">Submit</button>
                <button v-if="isNative" class="btn btn-secondary" @click="closeWindow">Cancel</button>
            </div>
        </form>
    </div>
</script>
<script>
  window.$MusicServer = [=modelJson]

  window.$app = Vue.createApp({
    components : {
      "LoginPagePrepare" : {
        template: `#LoginPrepareView`
      },
      "LoginPageForm" : {
        template: `#LoginPageFromView`,
        props: {
          session: {
            type: Object,
            default() {
              return {}
            }
          }
        },
        data() {
          return {
            form: {
              username: {
                value: null,
                rules: [
                  {
                    pattern: /^[A-Za-z0-9_\-.@]{3,64}$/,
                    message: "Username should in 3-64 chars, with alphabets, numbers, '_', '-', '.' or '@'."
                  }
                ],
                error: null
              },
              password: {
                value: null,
                rules: [
                  {pattern: /^.{5,128}$/, message: "Password should between 5 and 128 chars"}
                ],
                error: null,
              }
            },
          }
        },
        mounted() {
          console.debug(this.session)
        },
        methods: {
          checkForm(formItem) {
            const {value, rules} = formItem;
            for (const {pattern, message} of rules) {
              if (!pattern.test(value)) {
                formItem.error = message
                return
              }
            }
            formItem.error = null
          },
          closeWindow() {
            window.close()
          }
        },
        computed: {
          buttonDisabled() {
            const {username, password} = this.form
            return username.value == null || password.value == null || !!username.error || !!password.error
          },
          targetAction() {
            const {userAgent, scopes, redirect} = this.session
            const params = new URLSearchParams()
            params.append("user_agent", userAgent)
            for (const scope of scopes) {
              params.append("scope", scope)
            }
            params.append("redirect", redirect)
            return "/api/auth?" + params.toString()
          },
          isNative() {
            return !!(window.$native)
          }
        }
      },
    },
    data() {
      return {
        state: "login-page-prepare",
        session: $MusicServer.session,
      }
    },
    mounted() {
      setTimeout(() => {
        this.state = 'login-page-form'
      }, 1000)
    }
  })

  window.onload = (function(){ window.$app.mount("#app") })
</script>
</body>
</html>
