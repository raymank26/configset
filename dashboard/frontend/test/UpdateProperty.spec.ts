/**
 * @jest-environment jsdom
 */
import {mount} from "@vue/test-utils";
import Vue from 'vue'
import UpdateProperty from "@/components/UpdateProperty.vue";
import {applicationService, propertyService, searchService} from "@/service/services";
import ShowPropertyItem from "@/model/ShowPropertyItem";
import {UpdateResult} from "@/service/PropertyService";

jest.mock('@/service/services')
let listApplicationsMock = applicationService.listApplications as jest.Mock;
let propertyServiceMock = propertyService.readProperty as jest.Mock;
let updatePropertyMock = propertyService.updateProperty as jest.Mock;
let searchPushMock = searchService.searchPush as jest.Mock;

beforeEach(() => {
  listApplicationsMock.mockClear();
  propertyServiceMock.mockClear();
  updatePropertyMock.mockClear();
  searchPushMock.mockClear();
})

test('button is not active if permission is not granted', async () => {
  // given
  let mockRouter = {
    currentRoute: jest.fn() as any
  }
  mockRouter.currentRoute.query = {
    applicationName: "test"
  }
  listApplicationsMock.mockImplementation(() => {
    return Promise.resolve("test")
  })
  let wrapper = mount(UpdateProperty, {
    mocks: {
      $router: mockRouter,
      $roles: []
    }
  })
  await Vue.nextTick()

  // then
  expect(wrapper.find("#updateButton").attributes("disabled")).toBe("disabled")
});

test('button is active if permission is granted on change', async () => {
  // given
  let mockRouter = {
    currentRoute: jest.fn() as any
  }
  mockRouter.currentRoute.query = {
    applicationName: "test"
  }
  listApplicationsMock.mockImplementation(() => {
    return Promise.resolve(["test", "test-another"])
  })
  let wrapper = mount(UpdateProperty, {
    mocks: {
      $router: mockRouter,
      $roles: ["applicationOwner_test-another", "hostCreator"]
    }
  })
  await Vue.nextTick()
  expect(wrapper.find("#updateButton").attributes("disabled")).toBe("disabled")

  // when
  await wrapper.setData({"appName": "test-another"})
  await Vue.nextTick()

  // then
  expect(wrapper.find("#updateButton").attributes("disabled")).toBe(undefined)
});

test("property value is loaded if property data is provided", async () => {
  // given
  let mockRouter = {
    currentRoute: jest.fn() as any
  }
  mockRouter.currentRoute.query = {
    applicationName: "test",
    propertyName: "testName",
    hostName: "testHostname"
  }
  listApplicationsMock.mockImplementation(() => {
    return Promise.resolve(["test", "test-another"])
  })
  propertyServiceMock.mockImplementation(() => {
    return Promise.resolve<ShowPropertyItem>({
      applicationName: "test",
      propertyName: "testName",
      hostName: "testHostname",
      propertyValue: "some test value",
      version: 1
    })
  })
  let wrapper = mount(UpdateProperty, {
    mocks: {
      $router: mockRouter,
      $roles: ["applicationOwner_test-test", "hostCreator"]
    }
  })
  await Vue.nextTick();
  await Vue.nextTick();

  // then
  let propertyValue = (wrapper.find("#propertyValue").element as HTMLInputElement).value
  expect(propertyValue).toBe("some test value")
})

test("submit function should persist values", async () => {
  // given
  let mockRouter = {
    currentRoute: jest.fn() as any
  }
  mockRouter.currentRoute.query = {
    applicationName: "test",
    propertyName: "testName",
    hostName: "testHostname"
  }
  listApplicationsMock.mockImplementation(() => {
    return Promise.resolve(["test"])
  })
  propertyServiceMock.mockImplementation(() => {
    return Promise.resolve<ShowPropertyItem>({
      applicationName: "test",
      propertyName: "testName",
      hostName: "testHostname",
      propertyValue: "some test value",
      version: 1
    })
  })
  updatePropertyMock.mockImplementation(() => {
    return Promise.resolve(UpdateResult.OK)
  });
  let wrapper = mount(UpdateProperty, {
    mocks: {
      $router: mockRouter,
      $roles: ["applicationOwner_test-test", "hostCreator"]
    }
  })
  await Vue.nextTick();
  await Vue.nextTick();

  // when
  wrapper.find("#propertyValue").setValue("newValue");
  (wrapper.vm.$refs["appForm"] as any).submit();

  // then
  await Vue.nextTick();
  expect(updatePropertyMock).toBeCalled();
  expect(searchPushMock).toBeCalled()
})

