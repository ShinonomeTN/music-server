[#ftl output_format="JavaScript"]
(function () {
  const microRPCPattern = /^([A-Za-z0-9.\-_]+):?(.+)?$/
  function microRPC(value) {
    if(!value) return null
    const matching = value.match(microRPCPattern)
    if(!matching) return null
    const [ _, name, parameters ] = matching
    var parameterList = []
    if(!!parameters) parameterList = parameters.split(',').map(i => {
      const decoded = decodeURIComponent(i)
      const matching = decoded.match(/^\[(.+?)?]$/)
      if(!!matching) {
        const [_, values] = matching
        if(!values) return []
        return values.split(',')
      }
      return decoded
    })

    return {
      method: name,
      parameters : parameterList
    }
  }

  const errors = {
    "param_validation_error" : {
      languages: {
        "_" : "Parameter Validation Error"
      }
    },
    "invalid_credentials" : {
      languages: {
        "_" : "Login Failed"
      }
    },
    "session_expired" : {
      languages: {
        "_" : "Session Expired"
      }
    }
  }

  const messages = {
    "param_non_null_required" : {
      languages : {
        _ : ([name]) => `Parameter '${name}' cannot be null.`
      }
    },
    "invalid_username_or_password" : {
      languages: {
        _ : () => `Invalid Username or Password.`
      }
    },
    "current_session_is_expired" : {
      languages: {
        _ : () => `Current Session Is Expired.`
      }
    }
  }

  const multiLanguageSupported = (() => {
    if(!navigator) return false
    return navigator.languages;
  })()

  function parseError(error) {
    const item = errors[error]
    if(!item) return error
    const languages = item.languages
    if(!multiLanguageSupported) return languages["_"]
    for(const language of navigator.languages) {
      var value = languages[language]
      if(!!value) return value
    }
    return languages["_"]
  }

  function parseMessage(message) {
    const intent = microRPC(message)
    if(!intent) return message
    const { method, parameters } = intent
    const item = messages[method]
    if(!item) return message
    const languages = item.languages
    if(!multiLanguageSupported) return languages["_"](parameters)
    for(const language of navigator.languages) {
      var value = languages[language]
      if(!!value) return value(parameters)
    }
    return languages["_"](parameters)
  }

  window.$i18n = {
    translateError : function({ error, message }) {
      return {
        title : parseError(error),
        description : parseMessage(message)
      }
    },
    translateMessage : function (message) {
      return parseMessage(message)
    }
  }
})()