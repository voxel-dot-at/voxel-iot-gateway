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

import { reactive, type App } from "vue";

interface ResourceValue {
  val: unknown;
  supposed?: boolean;
}

interface MultiResourceValue {
  vals: Record<string, ResourceValue>;
  supposed?: boolean;
}

interface EndpointData {
  data: Record<string, ResourceValue | MultiResourceValue>;
  observed: Record<string, boolean>;
  compositeObserved: Record<string, boolean>;
}

interface SingleResourceNode {
  kind: "singleResource";
  id: string | number;
  value: unknown;
  type: string;
}

interface MultiResourceNode {
  kind: "multiResource";
  id: string | number;
  values: Record<string, unknown>;
  type: string;
}

interface ResourceInstanceNode {
  kind: "resourceInstance";
  id: number;
  value: unknown;
  type: string;
}

interface InstanceNode {
  kind: "instance";
  id?: number;
  resources: Array<SingleResourceNode | MultiResourceNode>;
}

interface ObjectNode {
  kind: "obj";
  instances: Record<string, unknown>;
}

interface RootNode {
  kind: "root";
  objects: Array<{ id: string; instances: Record<string, unknown> }>;
}

interface CompositeObject {
  paths: string[];
}

interface ObjectModel {
  id: string;
  instances: Record<string, unknown>;
}

type Node = SingleResourceNode | MultiResourceNode | ResourceInstanceNode | InstanceNode | ObjectNode | RootNode;

function isSingleResourceNode(node: Node): node is SingleResourceNode {
  return node.kind === "singleResource";
}

function isMultiResourceNode(node: Node): node is MultiResourceNode {
  return node.kind === "multiResource";
}

function isResourceInstanceNode(node: Node): node is ResourceInstanceNode {
  return node.kind === "resourceInstance";
}

function isInstanceNode(node: Node): node is InstanceNode {
  return node.kind === "instance";
}

function isObjectNode(node: Node): node is ObjectNode {
  return node.kind === "obj";
}

function isRootNode(node: Node): node is RootNode {
  return node.kind === "root";
}

class Store {
  state: Record<string, EndpointData>;

  /*
    State STRUCTURE :
    ================
    
    state { endpoint : {
      data : { "singleInstanceResourcepath":   {  isSingle:true, val: "a resource value", supposed: true },
               "multipleInstanceResourcepath": {  isSingle:false, vals: {
                                                    id1: { val: "a resource value", supposed: true },
                                                    id1: { val: "a resource value", supposed: true }
                                                  },
                                                  supposed: true
                                                },    
             },
      observed : { path(object/instance/resource/resourceinstance) : boolean }
      compositeObserved : { 'path1,path2,...' : boolean}
      }
    };  
   */

  constructor() {
    // Vue 3 reactive state
    this.state = reactive<Record<string, EndpointData>>({});
  }

  initState(endpoint: string): void {
    this.state[endpoint] = {
      data: {},
      observed: {},
      compositeObserved: {},
    };
  }

  data(endpoint: string, path: string): ResourceValue | MultiResourceValue | undefined {
    return this.state[endpoint]?.data[path];
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to a resource e.g. /3/0/1
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newResourceValue(
    endpoint: string,
    path: string,
    node: SingleResourceNode,
    supposed: boolean = false
  ): void {
    if (!this.state[endpoint]) {
      this.initState(endpoint);
    }
    this.state[endpoint].data[path] = {
      val: node.value,
      supposed,
    };
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to a resource e.g. /3/0/1
   * @param {Object} values the values of the resource
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newMultiResourceValue(
    endpoint: string,
    path: string,
    values: Record<string, unknown>,
    supposed: boolean = false
  ): void {
    if (!this.state[endpoint]) {
      this.initState(endpoint);
    }
    this.state[endpoint].data[path] = {
      vals: Object.entries(values).reduce(
        (acc, [id, val]) => {
          acc[id] = { val, supposed };
          return acc;
        },
        {} as Record<string, ResourceValue>
      ),
      supposed,
    };
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the resource instance e.g. /3/0/1/0
   * @param {*} value the value of the resource instance
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newResourceInstanceValue(
    endpoint: string,
    path: string,
    value: unknown,
    supposed: boolean = false
  ): void {
    if (!this.state[endpoint]) {
      this.initState(endpoint);
    }
    this.state[endpoint].data[path] = {
      val: value,
      supposed,
    };
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the resource instance e.g. /3/0/1
   * @param {*} value the value of the resource instance
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newResourceInstanceValueFromPath(
    endpoint: string,
    path: string,
    value: unknown,
    supposed: boolean = false
  ): void {
    this.newResourceInstanceValue(endpoint, path, value, supposed);
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the instance e.g. /3/0
   * @param {Object} resources the resources of the instance
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newInstanceValue(
    endpoint: string,
    path: string,
    resources: Array<SingleResourceNode | MultiResourceNode>,
    supposed: boolean = false
  ): void {
    if (!this.state[endpoint]) {
      this.initState(endpoint);
    }
    resources.forEach((resource) => {
      if (resource.kind === "singleResource") {
        this.newResourceValue(endpoint, `${path}/${resource.id}`, resource, supposed);
      } else if (resource.kind === "multiResource") {
        this.newMultiResourceValue(
          endpoint,
          `${path}/${resource.id}`,
          resource.values,
          supposed
        );
      }
    });
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the object e.g. /3
   * @param {Object} instances the instances of the object
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newObjectValue(
    endpoint: string,
    path: string,
    instances: Record<string, unknown>,
    supposed: boolean = false
  ): void {
    if (!this.state[endpoint]) {
      this.initState(endpoint);
    }
    Object.entries(instances).forEach(([id, instance]) => {
      this.newInstanceValue(endpoint, `${path}/${id}`, instance as Array<SingleResourceNode | MultiResourceNode>, supposed);
    });
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {Array} objects Array of objects
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newRootValue(endpoint: string, objects: ObjectModel[], supposed: boolean = false): void {
    objects.forEach((obj) => {
      this.newObjectValue(endpoint, "/" + obj.id, obj.instances, supposed);
    });
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the node (object, instance, resource or resource instance)
   * @param {Node} node the node value
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newNode(
    endpoint: string,
    path: string,
    node: Node,
    supposed: boolean = false
  ): void {
    if (isSingleResourceNode(node)) {
      this.newResourceValue(endpoint, path, node, supposed);
    } else if (isMultiResourceNode(node)) {
      this.newMultiResourceValue(endpoint, path, node.values, supposed);
    } else if (isResourceInstanceNode(node)) {
      this.newResourceInstanceValueFromPath(endpoint, path, node.value, supposed);
    } else if (isInstanceNode(node)) {
      this.newInstanceValue(endpoint, path, node.resources, supposed);
    } else if (isObjectNode(node)) {
      this.newObjectValue(endpoint, path, node.instances, supposed);
    } else if (isRootNode(node)) {
      this.newRootValue(endpoint, node.objects, supposed);
    } else {
      // This should never happen as we've covered all cases
      const _exhaustiveCheck: never = node;
      console.log(_exhaustiveCheck, " not yet supported");
    }
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {Record<string, Node>} nodes the nodes map {path:node Object}
   * @param {Boolean} supposed true means the value is supposed (not really send by the client)
   */
  newNodes(
    endpoint: string,
    nodes: Record<string, Node>,
    supposed: boolean = false
  ): void {
    Object.entries(nodes).forEach(([path, node]) => {
      this.newNode(endpoint, path, node, supposed);
    });
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the instance e.g. /3/0
   */
  removeInstanceValue(endpoint: string, path: string): void {
    const instancePath = path + "/";
    const s = this.state[endpoint];
    if (!s) return;
    Object.keys(s.data).forEach((p) => {
      if (p.startsWith(instancePath)) {
        delete s.data[p];
      }
    });
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {String} path path to the instance e.g. /3
   */
  removeObjectValue(endpoint: string, path: string): void {
    const objectPath = path + "/";
    const s = this.state[endpoint];
    if (!s) return;
    Object.keys(s.data).forEach((p) => {
      if (p.startsWith(objectPath)) {
        delete s.data[p];
      }
    });
  }

  setObserved(endpoint: string, path: string, observed: boolean): void {
    const o = this.state[endpoint]?.observed[path];
    if (!o) {
      this.state[endpoint]!.observed[path] = observed;
    } else {
      this.state[endpoint]!.observed[path] = observed;
    }
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {Array} nodes the nodes map {path:node Object}
   * @param {Boolean} supposed true means the paths are "composite" observed.
   */
  setCompositePathsObserved(endpoint: string, paths: string[], observed: boolean): void {
    const key = paths.join(",");
    const o = this.state[endpoint]?.compositeObserved[key];
    if (!o) {
      this.state[endpoint]!.compositeObserved[key] = observed;
    } else {
      this.state[endpoint]!.compositeObserved[key] = observed;
    }
  }

  /**
   * @param {CompositeObject} compositeObject
   * @returns {string} the key used in state.compositeObserved for the given compositeObject
   */
  compositeObjectToKey(compositeObject: CompositeObject): string {
    return compositeObject.paths.join(",");
  }

  /**
   * @param {String} endpoint endpoint of the client
   * @param {CompositeObject} compositeObject A composite object
   * @param {Boolean} supposed true means the paths are "composite" observed.
   */
  setCompositeObjectObserved(
    endpoint: string,
    compositeObject: CompositeObject,
    observed: boolean
  ): void {
    this.setCompositePathsObserved(endpoint, compositeObject.paths, observed);
  }
}

// create plugin which make data accessible on all vues
const _store = new Store();

const StorePlugin = {
  install(app: App): void {
    app.config.globalProperties.$store = _store;
  },
};

export default StorePlugin;
