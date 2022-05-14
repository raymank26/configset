package com.configset.dashboard.util

import com.configset.dashboard.ConfigurationUpdateError
import com.configset.dashboard.property.ImportError

class ExceptionMappingService {

    fun throwUpdateErrorToException(configurationUpdateError: ConfigurationUpdateError): Nothing {
        throw when (configurationUpdateError) {
            ConfigurationUpdateError.CONFLICT -> BadRequest("update.conflict")
            ConfigurationUpdateError.APPLICATION_NOT_FOUND -> BadRequest("application.not.found")
            ConfigurationUpdateError.PROPERTY_NOT_FOUND -> BadRequest("property.not.found")
            ConfigurationUpdateError.HOST_NOT_FOUND -> BadRequest("host.not.found")
        }
    }

    fun throwImportErrorToException(importError: ImportError): Nothing {
        throw when (importError) {
            ImportError.APPLICATION_NOT_FOUND -> BadRequest("application.not.found")
            ImportError.HOST_NOT_FOUND -> BadRequest("property.not.found")
            ImportError.PROPERTY_CONFLICT -> BadRequest("update.conflict")
            ImportError.ILLEGAL_FORMAT -> BadRequest("illegal.format")
        }
    }
}