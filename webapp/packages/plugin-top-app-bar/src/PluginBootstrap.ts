/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
import { AppScreenService } from '@cloudbeaver/core-app';
import { Bootstrap, injectable } from '@cloudbeaver/core-di';

import { PublicTopNavBar } from './TopNavBar/PublicTopNavBar.js';

@injectable()
export class PluginBootstrap extends Bootstrap {
  constructor(private readonly appScreenService: AppScreenService) {
    super();
  }

  override register(): void | Promise<void> {
    this.appScreenService.placeholder.add(PublicTopNavBar);
  }
}
