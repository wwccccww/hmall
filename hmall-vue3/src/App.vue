<script setup>
import SearchOverlay from './components/SearchOverlay.vue'
import AiAssistantWidget from './components/AiAssistantWidget.vue'
</script>

<template>
  <router-view />
  <SearchOverlay />
  <AiAssistantWidget />
</template>

<style>
/* Global styles already in style.css */
</style>
