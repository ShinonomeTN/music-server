[#ftl output_format="JavaScript"]
[#-- @ftlvariable name="ext" type="java.util.Map" --]
;(function () {
  if(!window.$MusicServer) window.$MusicServer = {}
  window.$MusicServer.scopeDescriptions = [=ext.scopeDescriptionsJson()]
})()