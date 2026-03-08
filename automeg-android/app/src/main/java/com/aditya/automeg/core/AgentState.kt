package com.aditya.automeg.core

data class AgentState(
    val screenOn: Boolean,
    val appInForeground: Boolean,
    val cooldownActive: Boolean //This is just a safety parameter which takes care of the agent won't stick out in a loop when it is replying too fast.
)
