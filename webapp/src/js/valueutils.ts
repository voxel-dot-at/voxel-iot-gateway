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

interface ResourceValue {
  val: unknown;
  supposed?: boolean;
}

interface MultiResourceValue {
  vals: Record<string, ResourceValue>;
  supposed?: boolean;
}

interface Resource {
  isSingle: boolean;
  val?: unknown;
  vals?: Record<string, ResourceValue>;
}

interface ObjLinkValue {
  objectId: number;
  objectInstanceId: number;
}

/**
 * @param {unknown} value the value of a resource or resource instance
 * @param {string} type the type of the resource
 * @returns {string} a string for this value
 */
export function valueToString(value: unknown, type: string): string {
  if (type === "objlnk") {
    const objLink = value as ObjLinkValue;
    return `${objLink.objectId}:${objLink.objectInstanceId}`;
  } else if (type === "time") {
    const date = new Date(value as number);
    return `${date.toDateString()} - ${date.toLocaleTimeString()} (${date.getTime() / 1000}s)`;
  } else {
    return String(value);
  }
}

/**
 * @param {Resource} resource the resource object
 * @param {string} type the type of the resource
 * @returns {string} a string for this value
 */
export function resourceToString(resource: Resource, type: string): string {
  if (resource.isSingle) {
    // single resource
    return valueToString(resource.val, type);
  } else {
    // multi resource
    const entries = Object.entries(resource.vals || {});
    const size = entries.length;
    if (size === 0) {
      return "0 instance";
    } else {
      const nbInstances = `${size} instance${size > 1 ? "s" : ""}`;
      const values = entries
        .map(([id, v]) => `${id}:${valueToString(v.val, type)}`)
        .join(", ");
      return `${nbInstances} [${values}]`;
    }
  }
}
