package com.mickstarify.zooforzotero.ZoteroAPI.Model

import com.google.gson.annotations.SerializedName

data class ZoteroUploadAuthorizationPojo(
    @SerializedName("url") val url: String,
    @SerializedName("params") val params: ParamsPojo,
    @SerializedName("uploadKey") val uploadKey: String
)

data class ParamsPojo(
    @SerializedName("key") val key: String,
    @SerializedName("acl") val acl: String,
    @SerializedName("Content-MD5") val content_MD5: String,
    @SerializedName("success_action_status") val success_action_status: String,
    @SerializedName("policy") val policy: String,
    @SerializedName("x-amz-algorithm") val x_amz_algorithm: String,
    @SerializedName("x-amz-credential") val x_amz_credential: String,
    @SerializedName("x-amz-date") val x_amz_date: String,
    @SerializedName("x-amz-signature") val x_amz_signature: String,
    @SerializedName("x-amz-security-token") val x_amz_security_token: String
) {
}