/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
import React from 'react';

export const ConnectionFormBaseActionsLoader = React.lazy(async () => {
  const { ConnectionFormBaseActions } = await import('./ConnectionFormBaseActions.js');
  return { default: ConnectionFormBaseActions };
});
