<template>
  <section></section>
</template>

<script>
  export default {
    install({viewRegistry}) {
      viewRegistry.addView({
        href: 'http://39.106.80.206:5050/index.html',
        label: '任务调度',
        order: 800,
        component: this
      });
    }
  };
</script>

<style>
</style>
