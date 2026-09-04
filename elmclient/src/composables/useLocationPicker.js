import { computed, ref } from 'vue';
import axios from 'axios';
import { toast } from '../utils/toast';

const AMAP_KEY = '24cce1eb31aec79422f44af47428fc8a';
const DEFAULT_LOCATION = '天津大学北洋园校区';
const STORAGE_VERSION = '2';

/**
 * 首页位置选择器的完整状态机。
 * 页面只负责渲染；地图请求、三级选择和本地持久化集中在这里。
 */
export function useLocationPicker() {
    const currentLocation = ref(DEFAULT_LOCATION);
    const showPicker = ref(false);
    const loading = ref(false);
    const locationData = ref([]);
    const currentLevel = ref(0);
    const locationLevels = ['请选择省份', '请选择城市', '请选择区域'];
    const selectedLocation = ref(emptyLocation());
    const pendingLocation = ref(emptyLocation());

    const displayLocation = computed(() => {
        const text = getDisplayText(selectedLocation.value);
        return text || currentLocation.value;
    });

    function showLocationPicker() {
        pendingLocation.value = { ...selectedLocation.value };
        showPicker.value = true;
        loadProvinces();
    }

    function hideLocationPicker() {
        showPicker.value = false;
        pendingLocation.value = { ...selectedLocation.value };
    }

    async function loadProvinces() {
        const districts = await loadDistrictChildren('中国');
        if (!districts) return;
        locationData.value = districts;
        currentLevel.value = 0;
    }

    async function loadCities(provinceCode, provinceName) {
        const districts = await loadDistrictChildren(provinceCode);
        if (!districts) return;
        locationData.value = districts;
        currentLevel.value = 1;
        pendingLocation.value = {
            province: provinceName,
            city: '',
            district: ''
        };
    }

    async function loadDistricts(cityCode, cityName) {
        const districts = await loadDistrictChildren(cityCode);
        if (!districts) return;
        locationData.value = districts;
        currentLevel.value = 2;
        pendingLocation.value.city = cityName;
        pendingLocation.value.district = '';
    }

    async function loadDistrictChildren(keyword) {
        loading.value = true;
        try {
            const response = await axios.get('https://restapi.amap.com/v3/config/district', {
                params: { key: AMAP_KEY, keywords: keyword, subdistrict: 1 }
            });
            const root = response?.data?.districts?.[0];
            if (response?.data?.status !== '1' || !Array.isArray(root?.districts)) {
                toast.error('位置数据暂时不可用，请稍后重试');
                return null;
            }
            return root.districts;
        } catch (error) {
            console.error('加载位置数据失败:', error);
            toast.error('位置数据加载失败，请检查网络');
            return null;
        } finally {
            loading.value = false;
        }
    }

    function switchLevel(level) {
        if (level >= currentLevel.value) return;
        if (level === 0) {
            pendingLocation.value.city = '';
            pendingLocation.value.district = '';
            loadProvinces();
            return;
        }
        pendingLocation.value.district = '';
        loadCities(pendingLocation.value.province, pendingLocation.value.province);
    }

    function selectLocation(item) {
        if (currentLevel.value === 0) {
            loadCities(item.adcode, item.name);
        } else if (currentLevel.value === 1) {
            loadDistricts(item.adcode, item.name);
        } else {
            pendingLocation.value.district = item.name;
        }
    }

    function isSelected(item) {
        const fields = ['province', 'city', 'district'];
        return pendingLocation.value[fields[currentLevel.value]] === item.name;
    }

    function confirmLocation() {
        const { province, city, district } = pendingLocation.value;
        if (!province) return toast.error('请先选择省份');
        if (!city) return toast.error('请先选择城市');
        if (!district) return toast.error('请先选择区域');

        selectedLocation.value = { ...pendingLocation.value };
        const displayText = getDisplayText(selectedLocation.value);
        localStorage.setItem('userLocation', JSON.stringify(selectedLocation.value));
        localStorage.setItem('userLocationDisplay', displayText);
        localStorage.setItem('userLocationSource', 'manual');
        localStorage.setItem('userLocationVersion', STORAGE_VERSION);
        currentLocation.value = displayText;
        hideLocationPicker();
    }

    function restoreSavedLocation() {
        const isCurrentVersion = localStorage.getItem('userLocationSource') === 'manual'
            && localStorage.getItem('userLocationVersion') === STORAGE_VERSION;
        if (!isCurrentVersion) return;

        try {
            const saved = JSON.parse(localStorage.getItem('userLocation'));
            if (!isCompleteLocation(saved)) return;
            selectedLocation.value = { ...saved };
            pendingLocation.value = { ...saved };
            currentLocation.value = localStorage.getItem('userLocationDisplay') || getDisplayText(saved);
        } catch (error) {
            console.warn('忽略无法解析的本地位置:', error);
        }
    }

    return {
        displayLocation,
        showPicker,
        loading,
        locationData,
        currentLevel,
        locationLevels,
        selectedLocation,
        showLocationPicker,
        hideLocationPicker,
        switchLevel,
        selectLocation,
        isSelected,
        confirmLocation,
        getDisplayText,
        restoreSavedLocation
    };
}

function emptyLocation() {
    return { province: '', city: '', district: '' };
}

function isCompleteLocation(location) {
    return location && typeof location.province === 'string'
        && typeof location.city === 'string'
        && typeof location.district === 'string';
}

function getDisplayText(location) {
    const { province, city, district } = location || emptyLocation();
    if (province && city && district) {
        return province === city ? `${province} ${district}` : `${province} ${city} ${district}`;
    }
    if (province && city) return `${province} ${city}`;
    return province || '';
}
