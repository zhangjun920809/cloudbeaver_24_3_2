/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
import type { IServiceProvider } from '@cloudbeaver/core-di';
import { FormState } from '@cloudbeaver/core-ui';

import type { ServerConfigurationFormService } from './ServerConfigurationFormService.js';

export class ServerConfigurationFormState extends FormState<null> {
  constructor(serviceProvider: IServiceProvider, service: ServerConfigurationFormService) {
    super(serviceProvider, service, null);
  }
}
