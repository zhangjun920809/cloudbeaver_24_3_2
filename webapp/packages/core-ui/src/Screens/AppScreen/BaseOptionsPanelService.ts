/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
import { action, makeObservable, observable } from 'mobx';

import { Executor, type IExecutor } from '@cloudbeaver/core-executor';

import type { OptionsPanelService } from './OptionsPanelService.js';

export abstract class BaseOptionsPanelService<T> {
  itemId: T | null;
  readonly onClose: IExecutor;

  constructor(
    protected readonly optionsPanelService: OptionsPanelService,
    private readonly panelGetter: () => React.FC,
  ) {
    this.itemId = null;
    this.onClose = new Executor();

    this.optionsPanelService.closeTask.next(this.onClose, undefined, () => this.optionsPanelService.isOpen(panelGetter));
    this.close = this.close.bind(this);

    makeObservable(this, {
      itemId: observable.ref,
      open: action,
      close: action,
    });
  }

  async open(itemId: T): Promise<boolean> {
    if (this.optionsPanelService.isOpen(this.panelGetter)) {
      return true;
    }

    const state = await this.optionsPanelService.open(this.panelGetter);

    if (state) {
      this.itemId = itemId;
    }

    return state;
  }

  async close(): Promise<void> {
    if (!this.optionsPanelService.isOpen(this.panelGetter)) {
      return;
    }

    const result = await this.optionsPanelService.close();

    if (result) {
      this.itemId = null;
    }
  }
}
