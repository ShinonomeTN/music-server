[#-- @ftlvariable name="path" type="String" --]
[#-- @ftlvariable name="modelJson" type="String" --]

[#attempt ]
    [#include "javascript/" + path + ".ftl"]
    [#recover ]
        [#assign suffix = path?matches("^.+(\\..+?)(?:\\?.+)?$")]
        [#if suffix]
            [#switch suffix]
                [#case '.js']
(function(){
alert('Fragment "[=path?json_string]" not found!. \n -- Music Server')
})()
                [#break ]
                [#case '.html']
                [#case '.htm']
<p>Fragment "[=path?json_string]" not found!. <br/> -- Music Server</p>
                [#break ]
                [#default ]
Fragment "[=path?json_string]" not found!.
-- Music Server
                [#break ]
            [/#switch]
        [#else ]
<p>Fragment "[=path?json_string]" not found!. <br/> -- Music Server</p>
        [/#if]
[/#attempt]
