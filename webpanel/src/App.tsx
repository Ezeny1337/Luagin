import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Layout, Menu, Select } from 'antd';
import { PieChartOutlined, SettingOutlined } from '@ant-design/icons';
import Performance from './components/Panel/Performance';
import Login from './components/Login';
import logo from './assets/logo.webp';
import './App.css';

const { Sider, Content } = Layout;

const App: React.FC = () => {
  const [logged, setLogged] = useState(false);
  const { t, i18n } = useTranslation();
  const bgColor = '#f0f2f5';
  const siderBg = '#fff';

  // 校验登录状态
  useEffect(() => {
    fetch('/api/perf/all', { credentials: 'include' })
      .then(res => {
        if (res.status === 401) {
          setLogged(false);
        } else {
          setLogged(true);
        }
      })
      .catch(() => setLogged(false));
  }, []);

  if (!logged) {
    return <Login onLogin={() => setLogged(true)} />;
  }
  return (
    <>
      <Sider
        theme='light'
        width={220}
        style={{
          padding: '0',
          background: siderBg,
          position: 'fixed',
          left: 0,
          top: 0,
          height: '100vh',
          zIndex: 10,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
        }}
      >
        <div className="sider-scroll">
          <div className="logo-title">
            <img src={logo} alt="logo" className="logo-img" />
            <span className="logo-text">{t('title')}</span>
          </div>
          <Menu
            theme='light'
            mode="inline"
            defaultSelectedKeys={['performance']}
            style={{ borderRight: 0, background: siderBg }}
            items={[
              {
                key: 'performance',
                icon: <PieChartOutlined />,
                label: t('performance'),
              },
              {
                key: 'config',
                icon: <SettingOutlined />,
                label: t('config'),
                disabled: true,
              },
            ]}
          />
        </div>
        <div className="theme-lang-switcher" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '16px 0', gap: 12 }}>
          <Select
            value={i18n.language}
            onChange={value => i18n.changeLanguage(value)}
            style={{ width: 100 }}
            options={[
              { value: 'zh', label: '中文' },
              { value: 'en', label: 'English' }
            ]}
            size="small"
          />
        </div>
      </Sider>
      <Layout style={{ marginLeft: 220 }}>
        <Content style={{ margin: 0, padding: 24, minHeight: '100vh', background: bgColor }}>
          <Performance />
        </Content>
      </Layout>
    </>
  );
};

export default App;