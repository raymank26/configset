import {uuidv4} from "@/service/Uuid";
import SearchPropertiesRequest from "@/model/SearchPropertiesRequest";
import ShowPropertyItem from "@/model/ShowPropertyItem";
import {axiosApiInstance} from "@/Axios";

const qs = require('querystring');

export default class PropertyService {

  updateProperty(appName: string, hostName: string, propertyName: string, propertyValue: string, version: number | null): Promise<UpdateResult> {
    let request = {
      "applicationName": appName,
      "hostName": hostName,
      "propertyName": propertyName,
      "propertyValue": propertyValue,
      "requestId": uuidv4()
    } as any;
    if (version) {
      request["version"] = version
    }
    return axiosApiInstance.post("/api/property/update", qs.stringify(request))
      .then(() => UpdateResult.OK)
      .catch(reason => {
        let code = reason.response.data.code;
        switch (reason.response.data.code) {
          case "update.conflict":
            return UpdateResult.CONFLICT;
          case "application.not.found":
            return UpdateResult.APPLICATION_NOT_FOUND;
          default:
            throw Error("Unhandled code = " + code)
        }
      })
  }

  searchProperties(searchPropertiesRequest: SearchPropertiesRequest): Promise<ShowPropertyItem[]> {
    let request = {
      "applicationName": searchPropertiesRequest.applicationName,
      "hostName": searchPropertiesRequest.searchHost,
      "propertyName": searchPropertiesRequest.searchPropertyName,
      "propertyValue": searchPropertiesRequest.searchPropertyValue,
    };
    return axiosApiInstance.get("/api/property/search", {
      params: request
    }).then(response => response.data)
  }

  readProperty(applicationName: string, hostName: string, propertyName: string): Promise<ShowPropertyItem | null> {
    let request = {
      "applicationName": applicationName,
      "hostName": hostName,
      "propertyName": propertyName,
    };
    return axiosApiInstance.get("api/property/get", {
      params: request
    }).then(response => response.data);
  }

  deleteProperty(applicationName: string, hostName: string, propertyName: string, version: number): Promise<DeleteResult> {
    let request = {
      "applicationName": applicationName,
      "hostName": hostName,
      "propertyName": propertyName,
      "version": version,
      "requestId": uuidv4()
    } as any;

    return axiosApiInstance.post("/api/property/delete", qs.stringify(request))
      .then(() => DeleteResult.OK)
      .catch(reason => {
        let code = reason.response.data.code;
        switch (reason.response.data.code) {
          case "delete.conflict":
            return DeleteResult.CONFLICT;
          default:
            throw Error("Unhandled code = " + code)
        }
      })
  }

  importConfiguration(applicationName: string, propertiesXml: string): Promise<ImportResult> {
    let request = {
      "applicationName": applicationName,
      "properties": propertiesXml,
      "requestId": uuidv4()
    };
    return axiosApiInstance.post("/api/property/import", qs.stringify(request))
      .then(() => ImportResult.OK)
      .catch(reason => {
        let code = reason.response.data.code;
        switch (reason.response.data.code) {
          case "illegal.format":
            return ImportResult.INVALID_FORMAT;
          default:
            throw Error("Unhandled code = " + code)
        }
      })
  }
}

export interface ReadPropertyResult {
  property: ShowPropertyItem | null
}

export enum UpdateResult {
  OK,
  CONFLICT,
  APPLICATION_NOT_FOUND,
}

export enum DeleteResult {
  OK,
  CONFLICT
}

export enum ImportResult {
  OK,
  INVALID_FORMAT
}
