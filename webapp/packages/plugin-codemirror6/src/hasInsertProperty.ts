/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
import type { ChangeSpec, Text } from '@codemirror/state';

export function hasInsertProperty(spec: ChangeSpec | undefined): spec is { from: number; to?: number; insert?: string | Text } {
  return typeof spec === 'object' && spec !== null && 'insert' in spec;
}
