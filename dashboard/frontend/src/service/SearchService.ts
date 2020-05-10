import router from "@/router";

export default class SearchService {

  searchReplace(searchParams: SearchParams) {
    router.replace({
      query: this.prepareParams(searchParams)
    }).catch(() => {
    });
  }

  searchPush(searchParams: SearchParams) {
    router.push({
      path: "search",
      query: this.prepareParams(searchParams)
    })
  }

  private prepareParams(searchParams: SearchParams) {
    return {
      applicationName: searchParams.searchApplicationName || undefined,
      host: searchParams.searchHost || undefined,
      propertyName: searchParams.searchPropertyName || undefined,
      propertyValue: searchParams.searchPropertyValue || undefined
    }
  }
}


export interface SearchParams {
  searchApplicationName: string | null
  searchHost: string | null
  searchPropertyName: string | null
  searchPropertyValue: string | null
}
