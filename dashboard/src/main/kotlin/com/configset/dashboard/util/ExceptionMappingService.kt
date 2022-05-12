package com.configset.dashboard.util

import com.configset.dashboard.UpdateError
import com.configset.dashboard.property.ImportError

class ExceptionMappingService {

    fun mapUpdateErrorToException(updateError: UpdateError): Exception {
        return when (updateError) {
            UpdateError.CONFLICT -> BadRequest("update.conflict")
            UpdateError.APPLICATION_NOT_FOUND -> BadRequest("application.not.found")
            UpdateError.PROPERTY_NOT_FOUND -> BadRequest("property.not.found")
            UpdateError.HOST_NOT_FOUND -> BadRequest("host.not.found")
        }
    }

    fun mapImportErrorToException(importError: ImportError): Exception {
        return when (importError) {
            ImportError.APPLICATION_NOT_FOUND -> BadRequest("application.not.found")
            ImportError.HOST_NOT_FOUND -> BadRequest("property.not.found")
            ImportError.ILLEGAL_FORMAT -> BadRequest("illegal.format")
        }
    }
}