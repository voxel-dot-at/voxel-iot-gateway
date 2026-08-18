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

/**
 * A class to handle LWM2M path
 */
export class LwM2mPath {
  path: string;
  type: string;
  objectid?: number;
  objectinstanceid?: number;
  resourceid?: number;
  resourceinstanceid?: number;

  /**
   * @param {string} path as a string
   */
  constructor(path: string) {
    this.path = path;

    if (path.indexOf("/") !== 0) {
      this.type = "invalid";
      return;
    }
    const pathPart = path.split("/");
    try {
      if (pathPart.length === 2) {
        if (path.length === 1) {
          this.type = "root";
        } else {
          this.type = "object";
          this.objectid = parseInt(pathPart[1], 10);
        }
      } else if (pathPart.length === 3) {
        this.type = "objectinstance";
        this.objectid = parseInt(pathPart[1], 10);
        this.objectinstanceid = parseInt(pathPart[2], 10);
      } else if (pathPart.length === 4) {
        this.type = "resource";
        this.objectid = parseInt(pathPart[1], 10);
        this.objectinstanceid = parseInt(pathPart[2], 10);
        this.resourceid = parseInt(pathPart[3], 10);
      } else if (pathPart.length === 5) {
        this.type = "resourceinstance";
        this.objectid = parseInt(pathPart[1], 10);
        this.objectinstanceid = parseInt(pathPart[2], 10);
        this.resourceid = parseInt(pathPart[3], 10);
        this.resourceinstanceid = parseInt(pathPart[4], 10);
      } else {
        this.type = "invalid";
      }
    } catch (error) {
      this.type = "invalid";
      console.log("invalid path", path, error);
    }
  }

  toResourcePath(): string {
    if (
      this.objectid === undefined ||
      this.objectinstanceid === undefined ||
      this.resourceid === undefined
    ) {
      throw new Error("Cannot convert to resource path: missing required properties");
    }
    return `/${this.objectid}/${this.objectinstanceid}/${this.resourceid}`;
  }

  toString(): string {
    return this.path;
  }
}
