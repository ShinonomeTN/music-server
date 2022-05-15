package com.shinonometn.music.server.commons

/** @restful_api_param_doc
 * @bean_name BusinessException
 * # Common business exception bean
 * | field name  | type    | required | description |
 * | ----------- | ------- | -------- | ----------- |
 * | error       | String  | true     | main error name |
 * | message     | String  | true     | error description, it may be represented in MicroRpc |
 *
 */
class BusinessException(message: String, val error : String = "business_error") : Exception(message)

fun businessError(message: String) : Nothing = throw BusinessException(message)

fun validationError(message: String) : Nothing = throw BusinessException(message, "validation_error")