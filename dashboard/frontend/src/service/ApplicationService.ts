import {uuidv4} from "@/service/Uuid";
import {axiosApiInstance} from "@/main";

const qs = require('querystring');

export class ApplicationService {

  createApplication(appName: string): Promise<any> {
    return axiosApiInstance.post("/api/application", qs.stringify({
      "appName": appName,
      "requestId": uuidv4()
    }))
  }

  listApplications(): Promise<string[]> {
    return axiosApiInstance.get("/api/application/list").then(data => data.data)
  }
}
