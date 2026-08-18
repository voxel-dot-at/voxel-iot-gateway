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
 * Helper function to convert data from UI Component format
 * To REST API format
 *
 * -----------------------------------------------------------------
 * /!\ maybe we should modify the REST API to limit this conversion ?
 * -----------------------------------------------------------------
 */

interface ResourceModel {
  id: string | number;
  type: string;
  instancetype: "single" | "multi";
}

interface InstanceValue {
  [key: string]: {
    id: string | number;
    val: unknown;
  };
}

interface InstanceData {
  kind: "instance";
  id?: number;
  resources: ResourceData[];
}

interface SingleResourceData {
  kind: "singleResource";
  id: string | number;
  value: unknown;
  type: string;
}

interface MultiResourceData {
  kind: "multiResource";
  id: string | number;
  type: string;
  values: Record<string, unknown>;
}

interface ResourceInstanceData {
  kind: "resourceInstance";
  id: number;
  value: unknown;
  type: string;
}

type ResourceData = SingleResourceData | MultiResourceData | ResourceInstanceData;

interface LwM2mPath {
  type: string;
  resourceinstanceid?: number;
}

/**
 * @param {Object} model model of the object.
 * @param {number} id the instance id.
 * @param {InstanceValue} value the values of the resource from UI Component.
 * @returns {InstanceData} a LWM2M object instance usable for REST API.
 */
export function instanceToREST(
  model: unknown,
  id: number | null,
  value: InstanceValue
): InstanceData {
  const data: InstanceData = { kind: "instance", resources: [] };

  // set ID
  if (id !== null) {
    data.id = id;
  }

  // set resources
  for (const key in value) {
    const resmodel = (model as { resourcedefs: ResourceModel[] }).resourcedefs.find(
      (m) => m.id === key
    );
    data.resources.push(resourceToREST(resmodel!, value[key]));
  }

  return data;
}

/**
 * @param {ResourceModel} model model of the resource.
 * @param {{ id: string | number; val: unknown }} value the value of the resource from UI Component.
 * @returns {ResourceData} a single instance OR multi instance resource usable for REST API.
 */
export function resourceToREST(
  model: ResourceModel,
  value: { id: string | number; val: unknown }
): ResourceData {
  if (model.instancetype === "single") {
    return singleInstanceResourceToREST(model, value);
  } else {
    // For multi-instance, wrap the single value in a record
    return multiInstanceResourceToREST(model, { [value.id as string]: value });
  }
}

/**
 * @param {ResourceModel} model model of the resource.
 * @param {{ id: string | number; val: unknown }} value the value of the resource from UI Component.
 * @returns {SingleResourceData} a single instance resource usable for REST API.
 */
function singleInstanceResourceToREST(
  model: ResourceModel,
  value: { id: string | number; val: unknown }
): SingleResourceData {
  const res: SingleResourceData = {
    id: model.id,
    kind: "singleResource",
    value: value.val,
    type: model.type,
  };
  return res;
}

/**
 * @param {ResourceModel} model model of the resource.
 * @param {Record<string, { id: string | number; val: unknown }>} value the value of the resource from UI Component.
 * @returns {MultiResourceData} multi instance resource usable for REST API.
 */
function multiInstanceResourceToREST(
  model: ResourceModel,
  value: Record<string, { id: string | number; val: unknown }>
): MultiResourceData {
  const res: MultiResourceData = {
    id: model.id,
    kind: "multiResource",
    type: model.type,
    values: Object.entries(value).reduce((resource, [_, instance]) => {
      resource[instance.id as string] = instance.val;
      return resource;
    }, {} as Record<string, unknown>),
  };
  return res;
}

/**
 * @param {ResourceModel} model model of the resource.
 * @param {number} instanceId the id of the resource instance
 * @param {unknown} value the value of the resource instance from UI Component.
 * @returns {ResourceInstanceData} a resource instance usable for REST API.
 */
export function resourceInstanceToREST(
  model: ResourceModel,
  instanceId: number,
  value: unknown
): ResourceInstanceData {
  const res: ResourceInstanceData = {
    id: instanceId,
    kind: "resourceInstance",
    value,
    type: model.type,
  };
  return res;
}

/**
 * @param {ResourceModel} model model of the resource.
 * @param {LwM2mPath} path the lwm2m path object
 * @param {unknown} value the value of the resource or resource instance from UI Component.
 * @returns {ResourceData} a resource instance or single resource usable for REST API.
 */
export function singleValueToREST(
  model: ResourceModel,
  path: LwM2mPath,
  value: unknown
): ResourceData {
  if (path.type === "resource" && model.instancetype === "single") {
    return singleInstanceResourceToREST(model, { id: 0, val: value });
  } else if (path.type === "resourceinstance") {
    if (path.resourceinstanceid === undefined) {
      throw new Error("resourceinstanceid is required for resourceinstance path type");
    }
    return resourceInstanceToREST(model, path.resourceinstanceid, value);
  }
  throw new Error(`Unsupported kind of node ${path.type}`);
}
