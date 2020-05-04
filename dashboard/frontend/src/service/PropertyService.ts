import Axios from "axios";
import {uuidv4} from "@/service/Uuid";
import SearchPropertiesRequest from "@/model/SearchPropertiesRequest";
import ShowPropertyItem from "@/model/ShowPropertyItem";

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
    return Axios.post("/api/property/update", qs.stringify(request))
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
    return Axios.get("/api/property/search", {
      params: request
    }).then(response => response.data)
  }

  readProperty(applicationName: string, hostName: string, propertyName: string): Promise<ShowPropertyItem> {
    return this.searchProperties(new SearchPropertiesRequest(applicationName, hostName, propertyName, null)).then(response => {
      return response[0]
    });
  }
}

export enum UpdateResult {
  OK,
  CONFLICT,
  APPLICATION_NOT_FOUND,

}
