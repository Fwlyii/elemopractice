import { ref, watch } from 'vue';

// 模拟的地址数据
const addressData = {
  '广东省': {
    '广州市': ['天河区', '海珠区', '越秀区'],
    '深圳市': ['南山区', '福田区', '宝安区']
  },
  '四川省': {
    '成都市': ['武侯区', '锦江区', '青羊区'],
    '绵阳市': ['涪城区', '游仙区']
  },
  '浙江省': {
    '杭州市': ['西湖区', '拱墅区', '萧山区'],
    '宁波市': ['海曙区', '江北区']
  }
};

export function useAddressPicker(addressForm) {
  const showAddressPickerModal = ref(false);
  const provinces = ref(Object.keys(addressData));
  const cities = ref([]);
  const districts = ref([]);
  const selectedProvince = ref('');
  const selectedCity = ref('');
  const selectedDistrict = ref('');

  const openAddressPicker = () => {
    showAddressPickerModal.value = true;
    selectedProvince.value = provinces.value[0] || '';
  };

  const closeAddressPicker = () => {
    showAddressPickerModal.value = false;
    addressForm.region = `${selectedProvince.value} ${selectedCity.value} ${selectedDistrict.value}`;
  };

  const selectProvince = (p) => {
    selectedProvince.value = p;
    selectedCity.value = '';
    selectedDistrict.value = '';
  };

  const selectCity = (c) => {
    selectedCity.value = c;
    selectedDistrict.value = '';
  };

  const selectDistrict = (d) => {
    selectedDistrict.value = d;
    closeAddressPicker();
  };

  // 监听省份和城市变化，更新城市和区县列表
  watch(selectedProvince, (newVal) => {
    if (newVal && addressData[newVal]) {
      cities.value = Object.keys(addressData[newVal]);
      selectedCity.value = cities.value[0] || '';
    } else {
      cities.value = [];
      selectedCity.value = '';
    }
  });

  watch(selectedCity, (newVal) => {
    if (newVal && addressData[selectedProvince.value] && addressData[selectedProvince.value][newVal]) {
      districts.value = addressData[selectedProvince.value][newVal];
      selectedDistrict.value = districts.value[0] || '';
    } else {
      districts.value = [];
      selectedDistrict.value = '';
    }
  });

  return {
    showAddressPickerModal,
    provinces,
    cities,
    districts,
    selectedProvince,
    selectedCity,
    selectedDistrict,
    openAddressPicker,
    closeAddressPicker,
    selectProvince,
    selectCity,
    selectDistrict,
    addressData
  };
}