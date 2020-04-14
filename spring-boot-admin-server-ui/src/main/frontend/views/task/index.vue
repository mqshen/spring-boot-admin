<template>
  <section></section>
</template>

<script>
  export default {
    install({viewRegistry}) {
      viewRegistry.addView({
        href: 'http://192.168.203.146:5050/index.html',
        label: '任务',
        order: 800,
        component: this
      });
    }
  };
</script>

<style>
</style>
