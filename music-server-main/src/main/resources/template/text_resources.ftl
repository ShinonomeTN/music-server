[#-- @ftlvariable name="path" type="String" --]
[#-- @ftlvariable name="modalJson" type="String" --]
[#if path == 'com.js']
    [#include "javascript/com.js.ftl"]
[/#if]

[#if path == 'i18n.js']
    [#include "javascript/i18n.js.ftl"]
[/#if]

[#if path == 'scope_descriptions.js']
    [#include "javascript/scope_descriptions.js.ftl"]
[/#if]