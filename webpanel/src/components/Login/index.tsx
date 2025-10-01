import React, { useState, useMemo } from 'react';
import { Form, Button, Card, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import './index.css';

interface LoginProps {
  onLogin: () => void;
}

function getRandom(min: number, max: number) {
  return Math.random() * (max - min) + min;
}

// 浮动正方形参数
const floatingSquares = [
  { left: '16%', top: '16%', width: 80, height: 80, zIndex: 1, above: false },
  { left: '62%', top: '8%', width: 60, height: 60, zIndex: 1, above: false },
  { left: '88%', top: '36%', width: 110, height: 110, zIndex: 1, above: false },
  { left: '28%', top: '75%', width: 100, height: 100, zIndex: 1, above: false },
  { left: '8%', top: '58%', width: 70, height: 70, zIndex: 1, above: false },
  { left: '72%', top: '65%', width: 50, height: 50, zIndex: 1, above: false },
  { left: '52%', top: '88%', width: 75, height: 75, zIndex: 1, above: false },
  { left: '58%', top: '29%', width: 90, height: 90, zIndex: 1, above: false },
  { left: '34%', top: '45%', width: 120, height: 120, zIndex: 3, above: true },
];

const Login: React.FC<LoginProps> = ({ onLogin }) => {
  const { t } = useTranslation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [userFocus, setUserFocus] = useState(false);
  const [passFocus, setPassFocus] = useState(false);

  const onFinish = async () => {
    try {
      const response = await fetch('/api/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username,
          password,
        }),
        credentials: 'include', // 允许cookie
      });

      if (response.ok) {
        const data = await response.json();
        // 设置token到cookie，30分钟
        document.cookie = `luagin_token=${data.token}; path=/; max-age=1800`;
        message.success(t('loginSuccess'));
        onLogin();
      } else {
        message.error(t('loginFail'));
      }
    } catch (error) {
      console.error('Login request failed:', error);
      message.error(t('loginNetworkFail'));
    }
  };

  // 只在挂载时生成动画参数
  const squareAnims = useMemo(
    () =>
      floatingSquares.map(() => ({
        animationDelay: `${getRandom(0, 1.2)}s`,
        animationDuration: `${getRandom(3.5, 5.5)}s`,
      })),
    []
  );

  return (
    <div className="login-container">
      {/* 卡片下方的正方形 */}
      <div className="floating-squares under">
        {floatingSquares
          .map((sq, i) => ({ ...sq, idx: i }))
          .filter((sq) => !sq.above)
          .map((sq) => (
            <div
              key={sq.idx}
              className="floating-square"
              style={{
                left: sq.left,
                top: sq.top,
                width: sq.width,
                height: sq.height,
                zIndex: sq.zIndex,
                ...squareAnims[sq.idx],
              }}
            />
          ))}
      </div>
      <Card className="login-card" style={{ zIndex: 2, position: 'relative' }}>
        <div className="login-title">Luagin</div>
        <Form
          name="normal_login"
          className="login-form"
          onFinish={onFinish}
        >
          <div className="floating-label-input">
            <span
              className={
                'floating-label' + ((userFocus || username) ? ' floating-label-active' : '')
              }
            >{t('username')}</span>
            <div className="input-affix-wrapper-custom">
              <UserOutlined className="site-form-item-icon" />
              <input
                className="ant-input"
                type="text"
                value={username}
                onChange={e => setUsername(e.target.value)}
                onFocus={() => setUserFocus(true)}
                onBlur={() => setUserFocus(false)}
                autoComplete="username"
              />
            </div>
          </div>
          <div className="floating-label-input">
            <span
              className={
                'floating-label' + ((passFocus || password) ? ' floating-label-active' : '')
              }
            >{t('password')}</span>
            <div className="input-affix-wrapper-custom">
              <LockOutlined className="site-form-item-icon" />
              <input
                className="ant-input"
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                onFocus={() => setPassFocus(true)}
                onBlur={() => setPassFocus(false)}
                autoComplete="current-password"
              />
            </div>
          </div>
          <Form.Item className="login-btn-item">
            <Button type="primary" htmlType="submit" className="login-form-button">
              {t('login')}
            </Button>
          </Form.Item>
        </Form>
      </Card>
      {/* 卡片上方的正方形 */}
      <div className="floating-squares above">
        {floatingSquares
          .map((sq, i) => ({ ...sq, idx: i }))
          .filter((sq) => sq.above)
          .map((sq) => (
            <div
              key={sq.idx}
              className="floating-square"
              style={{
                left: sq.left,
                top: sq.top,
                width: sq.width,
                height: sq.height,
                zIndex: sq.zIndex,
                ...squareAnims[sq.idx],
              }}
            />
          ))}
      </div>
    </div>
  );
};

export default Login; 