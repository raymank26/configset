import Axios from "axios";
import {uuidv4} from "@/service/Uuid";
import SearchPropertiesResult from "@/model/SearchPropertiesResult";
import SearchPropertiesRequest from "@/model/SearchPropertiesRequest";

const qs = require('querystring');

export default class PropertyService {

  updateProperty(appName: string, hostName: string, propertyName: string, propertyValue: string, version: string | null): Promise<any> {
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
  }

  searchProperties(searchPropertiesRequest: SearchPropertiesRequest): Promise<SearchPropertiesResult> {
    let request = {
      "applicationName": searchPropertiesRequest.applicationName,
      "hostName": searchPropertiesRequest.searchHost,
      "propertyName": searchPropertiesRequest.searchPropertyName,
      "propertyValue": searchPropertiesRequest.searchPropertyValue,
    };
    return Axios.get("/api/property/search", qs.stringify(request)).then(response => response.data)
  }
}
