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
 * @param {unknown} value the value to check
 * @returns {boolean} true if the value is a integer
 */
export function isInteger(value: unknown): boolean {
  if (typeof value === "number") {
    return value === Math.round(value);
  } else if (typeof value === "string" && value.trim() !== "") {
    return value === String(Math.round(Number(value)));
  }
  return false;
}

/**
 * @param {unknown} value the value to check
 * @returns {boolean} true if the value is a number
 */
export function isNumber(value: unknown): boolean {
  if (typeof value === "number") {
    return true;
  } else if (typeof value === "string" && value.trim() !== "") {
    return value === String(Number(value));
  }
  return false;
}
