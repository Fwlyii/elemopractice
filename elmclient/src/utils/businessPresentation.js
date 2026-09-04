const tagTone = label => {
  if (!label) return 'neutral';
  if (label.includes('满') || label.includes('购买') || label.includes('爱不释手')) return 'orange';
  if (label.includes('好评')) return 'gold';
  if (label.includes('新店') || label.includes('堂食')) return 'green';
  if (label.includes('买过') || label.includes('配送')) return 'blue';
  return 'neutral';
};

/** 前端不再推断标签资格，只展示后端规则引擎的结果。 */
export const getBusinessTags = business =>
  (Array.isArray(business?.recommendationTags) ? business.recommendationTags : [])
    .map(label => ({ label, tone: tagTone(label) }));

export const getRecommendationScore = business => Number(business?.recommendationScore) || 0;

export const hasConfiguredPromotion = business => {
  const threshold = Number(business?.promotionThreshold);
  const discount = Number(business?.promotionDiscount);
  return Number.isFinite(threshold) && Number.isFinite(discount)
    && threshold >= 1 && discount > 0 && discount < threshold;
};

export const supportsDineIn = business => [true, 1, '1', 'true'].includes(business?.dineInAvailable);
