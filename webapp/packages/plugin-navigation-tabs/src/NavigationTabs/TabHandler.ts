/*
 * CloudBeaver - Cloud Database Manager
 * Copyright (C) 2020-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0.
 * you may not use this file except in compliance with the License.
 */
import type { IExtension } from '@cloudbeaver/core-extensions';

import type { ITab } from './ITab.js';

export interface TabHandlerTabProps<T = any> {
  tab: ITab<T>;
  handler: TabHandler<T>;
  onSelect: (tabId: string) => void;
  onClose?: (tabId: string) => void;
}
export type TabHandlerTabComponent<T = any> = React.FunctionComponent<TabHandlerTabProps<T>>;

export interface TabHandlerPanelProps<T = any> {
  tab: ITab<T>;
  handler: TabHandler<T>;
}
export type TabHandlerPanelComponent<T = any> = React.FunctionComponent<TabHandlerPanelProps<T>>;

export type TabHandlerCloseEvent<T = any> = (tab: ITab<T>) => Promise<boolean> | boolean;
export type TabHandlerEvent<T = any> = (tab: ITab<T>) => Promise<void> | void;
export type TabSyncHandlerEvent<T = any> = (tab: ITab<T>) => void;
export type TabRestoreEvent<T = any> = (tab: ITab<T>) => Promise<boolean> | boolean;

export interface TabHandlerOptions<TState = any> {
  key: string;
  getTabComponent: () => TabHandlerTabComponent<TState>;
  getPanelComponent: () => TabHandlerPanelComponent<TState>;
  onSelect?: TabHandlerEvent<TState>;
  canClose?: TabHandlerCloseEvent<TState>;
  onClose?: TabHandlerEvent<TState>;
  onCloseSilent?: TabSyncHandlerEvent<TState>;
  onRestore?: TabRestoreEvent<TState>;
  onUnload?: TabHandlerEvent<TState>;
  extensions?: Array<IExtension<ITab<TState>>>;
}

export class TabHandler<TState = any> {
  key: string;
  getTabComponent: () => TabHandlerTabComponent<TState>;
  getPanelComponent: () => TabHandlerPanelComponent<TState>;
  onSelect?: TabHandlerEvent<TState>;
  onClose?: TabHandlerEvent<TState>;
  onCloseSilent?: TabSyncHandlerEvent<TState>;
  canClose?: TabHandlerCloseEvent<TState>;
  onRestore?: TabRestoreEvent<TState>;
  onUnload?: TabHandlerEvent<TState>;
  extensions?: Array<IExtension<ITab<TState>>>;

  constructor(options: TabHandlerOptions<TState>) {
    this.key = options.key;
    this.getTabComponent = options.getTabComponent;
    this.getPanelComponent = options.getPanelComponent;
    this.onSelect = options.onSelect;
    this.canClose = options.canClose;
    this.onClose = options.onClose;
    this.onCloseSilent = options.onCloseSilent;
    this.onRestore = options.onRestore;
    this.onUnload = options.onUnload;
    this.extensions = options.extensions;
  }
}
