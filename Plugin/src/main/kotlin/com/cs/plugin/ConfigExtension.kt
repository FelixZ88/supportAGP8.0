package com.cs.plugin

import org.gradle.api.logging.Logger

open class ConfigExtension {
    public var logTag: String = "cs"
    public var includePackages: Array<String> = arrayOf()
    public var includeMethods: Array<String> = arrayOf()
    public var excludePackages: Array<String> = arrayOf()
    public var excludeMethods: Array<String> = arrayOf()
    public var limitTime: Long = 500
}