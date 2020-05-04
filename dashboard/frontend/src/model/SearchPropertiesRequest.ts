export default class SearchPropertiesRequest {
  applicationName: string | null;
  searchHost: string | null;
  searchPropertyName: string | null;
  searchPropertyValue: string | null;

  constructor(applicationName: string | null, searchHost: string | null, searchPropertyName: string | null, searchPropertyValue: string | null) {
    this.applicationName = applicationName;
    this.searchHost = searchHost;
    this.searchPropertyName = searchPropertyName;
    this.searchPropertyValue = searchPropertyValue;
  }
}
