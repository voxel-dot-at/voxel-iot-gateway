/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *******************************************************************************/

import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse, type AxiosError } from "axios";
import type { App } from "vue";

// Full config:  https://github.com/axios/axios#request-config
// axios.defaults.baseURL = process.env.baseURL || process.env.apiUrl || '';
// axios.defaults.headers.common['Authorization'] = AUTH_TOKEN;
// axios.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded';

let config = {
  // baseURL: process.env.baseURL || process.env.apiUrl || ""
  // timeout: 60 * 1000, // Timeout
  // withCredentials: true, // Check cross-site Access-Control
  responseType: "json" as const,
};

// HACK waiting we get a solution for : https://github.com/yariksav/vuetify-dialog/issues/110#issuecomment-1145981361
// and unfortenately there is not standard way to do that ... : https://stackoverflow.com/questions/40263803/native-javascript-or-es6-way-to-encode-and-decode-html-entities
const escapeHTML = (str: string): string =>
  str.replace(
    /[&<>'"]/g,
    (tag) =>
      ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "'": "&#39;",
        '"': "&quot;",
      }[tag] || tag)
  );

const _axios: AxiosInstance = axios.create(config);

_axios.interceptors.request.use(
  function (config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
    // Do something before request is sent
    return config;
  },
  function (error: AxiosError): Promise<AxiosError> {
    // Do something with request error
    return Promise.reject(error);
  }
);

// Add a response interceptor
_axios.interceptors.response.use(
  function (response: AxiosResponse): AxiosResponse {
    // show error message if device return a failure code
    if (response.data && (response.data as { failure?: boolean }).failure) {
      let msg = `Device response : ${(response.data as { status?: string }).status}`;
      if ((response.data as { errormessage?: string }).errormessage)
        msg += ` - ${(response.data as { errormessage?: string }).errormessage}`;
      // Will be handled by global error handler
    }
    return response;
  },
  function (error: AxiosError): Promise<AxiosError> {
    let message: string;
    if (error.response) {
      console.log(
        `${error.message}[${error.response.status}], full error :`,
        error
      );
      if (typeof error.response.data === "string") {
        message = error.response.data;
      } else if (
        typeof error.response.data === "object" &&
        error.response.data !== null &&
        typeof (error.response.data as { message?: string }).message === "string"
      ) {
        message = (error.response.data as { message?: string }).message!;
      } else {
        message = error.message;
      }
    } else if (error.request) {
      console.log(`${error.message}:, full error :`, error);
      message =
        typeof (error.request as { data?: string }).data === "string"
          ? (error.request as { data?: string }).data!
          : error.message;
    } else {
      console.log(`${error.message}:, full error :`, error);
      message = error.message;
    }
    return Promise.reject(error);
  }
);

const axiosPlugin = {
  install(app: App): void {
    app.config.globalProperties.axios = _axios;
    app.config.globalProperties.$axios = _axios;
    (window as any).axios = _axios;
  },
};

export default axiosPlugin;
