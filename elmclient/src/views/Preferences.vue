<template>
  <div class="preferences-page">
    <header class="page-header">
      <h1>偏好与外观</h1>
    </header>

    <main class="preferences-content">
      <section class="settings-card appearance-card">
        <div class="card-heading">
          <div>
            <h2>外观主题</h2>
            <p>选择你习惯的页面显示方式</p>
          </div>
          <span class="heading-icon"><i class="fas fa-palette"></i></span>
        </div>
        <div class="theme-grid" role="radiogroup" aria-label="页面主题">
          <button
            v-for="option in themeOptions"
            :key="option.value"
            type="button"
            class="theme-choice"
            :class="['theme-' + option.value, { selected: preference.theme === option.value }]"
            :aria-pressed="preference.theme === option.value"
            @click="preference.theme = option.value"
          >
            <span class="theme-preview" aria-hidden="true">
              <i></i><i></i><i></i>
            </span>
            <span class="theme-copy">
              <strong>{{ option.label }}</strong>
              <small>{{ option.description }}</small>
            </span>
            <i v-if="preference.theme === option.value" class="fas fa-check-circle theme-check" aria-hidden="true"></i>
          </button>
        </div>
        <p class="field-hint">主题只影响页面显示，不会改变订单和资产数据。</p>
      </section>

      <section class="settings-card">
        <div class="card-heading">
          <div>
            <h2>点餐偏好</h2>
            <p>用于智能点餐和商家推荐，可随时修改</p>
          </div>
          <span class="heading-icon"><i class="fas fa-sliders-h"></i></span>
        </div>
        <label class="field-row" for="spicy-select">
          <span>辣度</span>
          <select id="spicy-select" v-model.number="preference.spicyLevel">
            <option :value="0">不吃辣</option>
            <option :value="1">微辣</option>
            <option :value="2">中辣</option>
            <option :value="3">重辣</option>
          </select>
        </label>
        <label class="input-field">
          <span>喜欢的口味</span>
          <input v-model="preference.tasteTags" maxlength="200" placeholder="如：清淡、面食、少油">
        </label>
        <label class="input-field">
          <span>忌口</span>
          <input v-model="preference.avoidTags" maxlength="200" placeholder="如：香菜、花生">
        </label>
        <label class="input-field">
          <span>偏好品类</span>
          <input v-model="preference.categoryTags" maxlength="200" placeholder="如：早餐、快餐、甜品">
        </label>
        <p class="field-hint">偏好只作为推荐参考，不会屏蔽其他商品，也不会影响历史订单。</p>
      </section>

      <div class="actions">
        <button class="primary-button" :disabled="saving" @click="savePreference">{{ saving ? '保存中…' : '保存设置' }}</button>
        <button class="secondary-button" :disabled="saving" @click="clearPreference">恢复默认</button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { applyTheme, getStoredTheme, normalizeTheme, THEME_OPTIONS } from '../utils/theme';

const defaultPreference = () => ({
  theme: getStoredTheme(),
  spicyLevel: 0,
  tasteTags: '',
  avoidTags: '',
  categoryTags: ''
});

const preference = ref(defaultPreference());
const saving = ref(false);
const themeOptions = THEME_OPTIONS;

const mergePreference = (value) => ({
  ...defaultPreference(),
  ...(value || {}),
  theme: normalizeTheme(value?.theme),
  spicyLevel: Number(value?.spicyLevel || 0)
});

const loadPreference = async () => {
  try {
    const response = await request.get('/api/v1/preferences/me');
    if (response.success) preference.value = mergePreference(response.data);
  } catch (error) {
    console.error('加载偏好失败', error);
    toast.error('偏好设置加载失败');
  }
};

const savePreference = async () => {
  saving.value = true;
  try {
    const response = await request.put('/api/v1/preferences/me', preference.value);
    if (response.success) {
      preference.value = mergePreference(response.data || preference.value);
      applyTheme(preference.value.theme);
      toast.success('设置已保存');
    } else {
      toast.error(response.message || '设置保存失败');
    }
  } catch (error) {
    toast.error(error?.message || '设置保存失败');
  } finally {
    saving.value = false;
  }
};

const clearPreference = async () => {
  saving.value = true;
  try {
    const response = await request.delete('/api/v1/preferences/me');
    if (response.success) {
      preference.value = { ...defaultPreference(), theme: 'light' };
      applyTheme('light');
      toast.success('已恢复默认设置');
    } else {
      toast.error(response.message || '恢复默认失败');
    }
  } catch (error) {
    toast.error(error?.message || '恢复默认失败');
  } finally {
    saving.value = false;
  }
};

watch(() => preference.value.theme, (theme) => applyTheme(theme), { immediate: true });
onMounted(loadPreference);
</script>

<style scoped>
.preferences-page{min-height:100vh;background:#f5f8fc;color:#29445d}
.page-header{height:56px;background:#168bd1;color:#fff;display:flex;align-items:center;padding:0 16px 0 64px;gap:12px;position:sticky;top:0;z-index:2;box-shadow:0 2px 8px rgba(25,104,156,.12)}
.back-button{border:0;background:none;color:#fff;font-size:30px;line-height:1;padding:0 4px;cursor:pointer}
.page-header h1{font-size:18px;margin:0;font-weight:600}
.preferences-content{max-width:640px;margin:auto;padding:18px 16px 104px}
.settings-card{background:#fff;border:1px solid #e0ebf4;border-radius:12px;padding:18px 20px;margin-bottom:12px;box-shadow:0 3px 12px rgba(45,95,130,.06)}
.card-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:14px}
.card-heading h2{font-size:16px;margin:0 0 5px;color:#29445d}
.card-heading p{font-size:12px;color:#8094a5;margin:0;line-height:1.5}
.heading-icon{width:34px;height:34px;border-radius:9px;background:#eaf5fc;color:#168bd1;display:flex;align-items:center;justify-content:center;flex:none}
.field-row{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:11px 0;border-top:1px solid #eef3f7;color:#5f778d;font-size:13px}
.field-row select{min-width:108px;border:1px solid #d8e6f1;border-radius:6px;padding:8px 10px;color:#29445d;background:#fbfdff}
.field-hint{font-size:12px;line-height:1.6;color:#8aa0b2;margin:11px 0 0}
.theme-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;margin-top:4px}
.theme-choice{position:relative;display:flex;align-items:center;gap:10px;min-width:0;padding:11px 12px;text-align:left;border:1px solid #dfeaf3;border-radius:9px;background:#fbfdff;color:#29445d;cursor:pointer;transition:border-color .18s,box-shadow .18s,background .18s}
.theme-choice:hover{border-color:#9fcbea;background:#f5fbff}
.theme-choice.selected{border-color:#168bd1;background:#f1f9ff;box-shadow:0 0 0 2px rgba(22,139,209,.1)}
.theme-preview{display:flex;flex-direction:column;gap:3px;width:34px;height:34px;padding:5px;box-sizing:border-box;border-radius:7px;background:#eaf5fc;flex:none}
.theme-preview i{display:block;height:6px;border-radius:3px;background:#168bd1}
.theme-preview i:nth-child(2){width:75%;background:#fff;border:1px solid #d8e6f1}
.theme-preview i:nth-child(3){width:55%;background:#9fcbea}
.theme-dark .theme-preview{background:#17283a}.theme-dark .theme-preview i{background:#76bde9}.theme-dark .theme-preview i:nth-child(2){background:#29445a;border-color:#36536b}.theme-dark .theme-preview i:nth-child(3){background:#9fb2c2}
.theme-mint .theme-preview{background:#e8f8f2}.theme-mint .theme-preview i{background:#159a78}.theme-mint .theme-preview i:nth-child(2){background:#fff;border-color:#cbe9dd}.theme-mint .theme-preview i:nth-child(3){background:#8bd1b6}
.theme-warm .theme-preview{background:#fff3df}.theme-warm .theme-preview i{background:#df8a2e}.theme-warm .theme-preview i:nth-child(2){background:#fff;border-color:#f1ddbd}.theme-warm .theme-preview i:nth-child(3){background:#e9bd78}
.theme-copy{display:grid;gap:3px;min-width:0}.theme-copy strong{font-size:13px;font-weight:600;white-space:nowrap}.theme-copy small{font-size:11px;color:#8094a5;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.theme-check{margin-left:auto;color:#168bd1;font-size:16px;flex:none}
.input-field{display:block;border-top:1px solid #eef3f7;padding:11px 0 0;margin-top:10px;color:#5f778d;font-size:13px}
.input-field span{display:block;margin-bottom:7px}
.input-field input{width:100%;box-sizing:border-box;border:1px solid #d8e6f1;border-radius:6px;padding:9px 10px;color:#29445d;background:#fbfdff;font-size:13px}
.input-field input:focus,.field-row select:focus{outline:2px solid rgba(22,139,209,.16);border-color:#168bd1}
.actions{display:flex;gap:10px;margin-top:4px}
.actions button{border:0;border-radius:7px;padding:10px 16px;font-size:13px;cursor:pointer}
.primary-button{background:#168bd1;color:#fff;flex:1}
.secondary-button{background:#eaf2f7;color:#5e7587}
.actions button:disabled{opacity:.6;cursor:wait}
@media(max-width:430px){.theme-grid{grid-template-columns:1fr}.theme-choice{padding:10px}.theme-copy small{white-space:normal}}
@media(max-width:375px){.preferences-content{padding-left:12px;padding-right:12px}.settings-card{padding:16px}.card-heading p{max-width:230px}.field-row select{min-width:96px}}
</style>
