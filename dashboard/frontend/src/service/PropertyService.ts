import Axios from "axios";
import {uuidv4} from "@/service/Uuid";

const qs = require('querystring');

export default class PropertyService {

  updateProperty(appName: string, hostName: string, propertyName: string, propertyValue: string, version: string | null): Promise<any> {
    let request = {
      "applicationName": appName,
      "hostName": hostName,
      "propertyName": propertyName,
      "propertyValue": propertyValue,
      "requestId": uuidv4()
    };
    if (version) {
      request["version"] = version
    }
    return Axios.post("/api/property/update", qs.stringify(request))
  }
}
