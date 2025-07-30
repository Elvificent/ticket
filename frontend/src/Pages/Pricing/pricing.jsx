import React from 'react';
import './pricing.css';
import ComparePlans from './ComparePlans'
import FAQ from './FAQ'

const Pricing = () => {
  const plans = [
    {
      title: 'Starter',
      price: 'Free',
      features: [
        'AI Assistant (basic)',
        'Submit up to 3 tickets/month',
        'Track ticket status in real time',
        'Access self-service knowledge base',
      ],
      button: 'Start for Free',
    },
    {
      title: 'Professional',
      price: '$29/mo',
      features: [
        'AI Support Assistant (advanced)',
        'Unlimited support tickets',
        'Auto escalation to Level 2 & 3 engineers',
        'Smart engineer dispatch by skill match',
        'Manager dashboard with KPI insights',
      ],
      button: 'Subscribe Now',
    },
    {
      title: 'Enterprise',
      price: 'Custom',
      features: [
        'Full in-house system integration',
        'Private AI model with RAG pipeline',
        'Advanced alerts and performance analytics',
        'Tailored SLAs and dedicated support team',
        'Admin console for role and system control',
      ],
      button: 'Contact Sales',
    },
  ];

  return (
    <div className="pricing-container">
      <h2 className="pricing-title">Flexible Plans for Smart Customer Service</h2>
      <p className="pricing-subtitle">Scale your support with AI + Human expertise</p>
      <div className="pricing-cards">
        {plans.map((plan, index) => (
          <div key={index} className="pricing-card">
            <h3>{plan.title}</h3>
            <p className="price">{plan.price}</p>
            <ul>
              {plan.features.map((feature, i) => (
                <li key={i}>{feature}</li>
              ))}
            </ul>
            <button>{plan.button}</button>
          </div>
        ))}
      </div>
      <ComparePlans/>
      <FAQ/>
    </div>
  );
};

export default Pricing;


