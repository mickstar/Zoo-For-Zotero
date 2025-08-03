package com.mickstarify.zooforzotero.ZoteroStorage.domain

/**
 * Exception thrown when Zotero API credentials are not available.
 * 
 * This exception indicates that the user needs to authenticate or re-authenticate
 * with the Zotero service before API operations can be performed.
 */
class NoCredentialsException(
    message: String = "No credentials available to connect to the Zotero server. Please re-authenticate."
) : Exception(message)