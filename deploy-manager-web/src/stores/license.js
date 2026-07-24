import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getLicenseList } from '@/api/license'

export const useLicenseStore = defineStore('license', () => {
  const licenseList = ref([])
  const loading = ref(false)

  async function fetchLicenseList(params = {}) {
    loading.value = true
    try {
      const res = await getLicenseList(params)
      licenseList.value = res?.records || res || []
      return licenseList.value
    } catch (e) {
      console.error(e)
      licenseList.value = []
      return []
    } finally {
      loading.value = false
    }
  }

  return {
    licenseList,
    loading,
    fetchLicenseList
  }
})
