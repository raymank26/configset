import Axios from "axios";
import {uuidv4} from "@/service/Uuid";

const qs = require('querystring');

export class ApplicationService {

  createApplication(appName: string): Promise<any> {
    return Axios.post("/api/application", qs.stringify({
      "appName": appName,
      "requestId": uuidv4()
    }))
  }

  listApplication(): Promise<string[]> {
    return Axios.get("/api/application/list").then(data => data.data)
  }
}
